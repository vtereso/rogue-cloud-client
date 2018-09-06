package com.roguecloud.client.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import com.roguecloud.ActionResponseFuture;
import com.roguecloud.Position;
import com.roguecloud.actions.CombatAction;
import com.roguecloud.actions.DrinkItemAction;
import com.roguecloud.actions.EquipAction;
import com.roguecloud.actions.IAction;
import com.roguecloud.actions.IActionResponse;
import com.roguecloud.actions.MoveInventoryItemAction;
import com.roguecloud.actions.MoveInventoryItemAction.Type;
import com.roguecloud.actions.MoveInventoryItemActionResponse;
import com.roguecloud.actions.NullAction;
import com.roguecloud.actions.NullActionResponse;
import com.roguecloud.actions.StepAction;
import com.roguecloud.client.IEventLog;
import com.roguecloud.client.RemoteClient;
import com.roguecloud.client.SelfState;
import com.roguecloud.client.WorldState;
import com.roguecloud.creatures.ICreature;
import com.roguecloud.events.CombatActionEvent;
import com.roguecloud.events.IEvent.EventType;
import com.roguecloud.items.Armour;
import com.roguecloud.items.DrinkableItem;
import com.roguecloud.items.IGroundObject;
import com.roguecloud.items.IObject;
import com.roguecloud.items.IObject.ObjectType;
import com.roguecloud.items.OwnableObject;
import com.roguecloud.items.Weapon;
import com.roguecloud.map.IMap;
import com.roguecloud.map.Tile;
import com.roguecloud.utils.AIUtils;
import com.roguecloud.utils.AStarSearch;
import com.roguecloud.utils.FastPathSearch;

public class RogueHelper {

    // Grabs the first health potion within list of visible ground objects
    // CAN BE USED WITHIN shouldIPickUpItem function
    public IGroundObject grabHealthPotion (List<IGroundObject> allVisibleGroundObjects) {
        IGroundObject bestPotion=null;
        for(IGroundObject visibleGroundObjectContainer : allVisibleGroundObjects) {
            if(objectOnGround.getObjectType() == ObjectType.ITEM && ) {
                DrinkableItem pot = (DrinkableItem)objectOnGround;
                if(pot.getEffect().getType().getPositiveEffect() == "Healing"){
                    //Is this really the best potion?? 
                    //DrinkableItem.java??
                    bestPotion = visibleGroundObjectContainer;
                }
            }
        }
        return bestPotion;
    }

    //  Does not check for HP
    //  Does not account for how many enemies are in the area (Single enemy)
    //  This function determines your "CRUSH" level against an enemy by checking your weapons and armor against each other
    //  Significance is determined by the point system
    // CAN BE USED WITHIN unprovokedAttackShouldIAttackBack function 
    // CAN BE USED WITHIN shouldIAttackCreature function
    public int isGoodFight(ICreature c) {
        ICreature player = getSelfState().getPlayer();
        Weapon myWeapon = player.getWeapon();
        int myWeaponRating = (myWeapon == null) ? 0 : myWeapon.calculateWeaponRating()*10;
        int myTotalArmour=0;
        for ( Armour a : player.getArmour().getAll()) {myTotalArmour=a.getDefense();};

        Weapon enemyWeapon = c.getWeapon();
        int enemyWeaponRating = (enemyWeapon == null) ? 0 : enemyWeapon.calculateWeaponRating()*10;
        int enemyTotalArmour=0;
        for ( Armour a : player.getArmour().getAll()) {enemyTotalArmour=a.getDefense();};

        // Each level is important as 3 defense 
        // Experiment with this value?
        int levelImportance=300;

        
        int myCrush = myWeaponRating - (enemyTotalArmour*100);
        int enemyCrush = enemyWeaponRating - (myTotalArmour*100);
        return (myCrush+player.getLevel()*levelImportance) - (enemyCrush+c.getLevel()*levelImportance);
    }

    //  Return the best equippable of the list
    //  Significance determined by the point system
    // CAN BE USED WITHIN shouldIPickUpItem function
    public IGroundObject grabBestEquip (List<IGroundObject> allVisibleGroundObjects) {
        ICreature player = selfState.getPlayer();

        IGroundObject bestEquip=null;
        int e_rating=0;

        for(IGroundObject visibleGroundObjectContainer : allVisibleGroundObjects) {
            IObject objectOnGround = visibleGroundObjectContainer.get();
            if(objectOnGround.getObjectType() == ObjectType.ARMOUR){
                Armour a = (Armour)objectOnGround;
                Armour currentSlot=player.getArmour().get(a.getType());
                int def=a.getDefense();
                int current_def=(currentSlot == null) ? 0 : currentSlot.getDefense();
                if(def > current_def){
                    int potential_gain=(def-current_def)*100;
                    if (a.getType().getName() == "Shield" && player.getWeapon() !=null && player.getWeapon().getType().getName() == "Two-handed"){
                        potential_gain-=(player.getWeapon().calculateWeaponRating()*10);
                    }
                    if(potential_gain > e_rating){
                        bestEquip=visibleGroundObjectContainer;
                        e_rating=potential_gain;
                    }
                }
            }
            else if(objectOnGround.getObjectType() == ObjectType.WEAPON) {
                Weapon w = (Weapon)objectOnGround;
                Weapon currentWeapon = player.getWeapon();
                int rating_diff=(w.calculateWeaponRating() - ((currentWeapon == null) ? 0 : currentWeapon.calculateWeaponRating()))*10;
                if(player.getArmour().get(ArmourType.SHIELD) != null && w.getType() == WeaponType.TWO_HANDED){
                        rating_diff-=(player.getArmour().get(ArmourType.SHIELD).getDefense()*100);
                }
                if (rating_diff > w_rating){
                        bestEquip = visibleGroundObjectContainer;
                        e_rating=rating_diff;
                }
            }
        }
        return bestEquip;
    }

    // Given a list of ICreatures determine how many variables you willing to overlook and opt to fight
    // E.g. too many creatures might influence your decision 
    // CAN BE USED WITHIN unprovokedAttackShouldIAttackBack function 
    // CAN BE USED WITHIN shouldIAttackCreature function
    public boolean shouldFight(List<ICreature> visibleMonsters) {
        ICreature player = getSelfState().getPlayer();

        // If this drops below zero, do not fight
        int allowances=10;
        for(ICreature c : visibleMonsters) {
            // Some maybe good, some maybe bad??
            // Comment some out the ones you think are bad or add your own :)
            // Maybe play with the allowances??
            // Check out the ICreature class!
            allowances+=(player.getLevel()-c.getLevel());
            if(c.getHp() > player.getHp())allowances-=3;
            if(c.getWeapon() != null && player.getWeapon() == null)allowances-=5;
        }
        // Are these a good idea??
        if(visibleMonsters.size() > 10)return false;
        // Maybe this is too much HP?
        if(player.getHp() < 100)return false;


        if(allowances > 0)return true;
        return false;
    }

    // Grab the first two handed weapon you see
    // Null otherwise
    // CAN BE USED WITHIN shouldIPickUpItem function
    public IGroundObject grabTwoHanded (List<IGroundObject> allVisibleGroundObjects) {
        for(IGroundObject visibleGroundObjectContainer : allVisibleGroundObjects) {
            IObject objectOnGround = visibleGroundObjectContainer.get();
            if(objectOnGround.getObjectType() == ObjectType.WEAPON) {
                Weapon w = (Weapon)objectOnGround;
                if(w.getType() == WeaponType.TWO_HANDED){
                    return visibleGroundObjectContainer;
                }
            }
        }
        return null;
    }
}

