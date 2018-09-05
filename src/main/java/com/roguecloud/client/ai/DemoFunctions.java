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
public class DemoFunctions {
    /** 
	 * This method is given the list of ALL items on the ground in your current view, and is asked, do you want to pick any of these up?
	 * If you see an item in the list that you want to pick up, return the IGroundObject that contains it!
	 **/
    public static IGroundObject shouldIPickUpItem(SimpleAI client, List<IGroundObject> allVisibleGroundObjects) {

		SelfState selfState = client.getSelfState();
		WorldState worldState = client.getWorldState();

		// Look at all the objects the agent can see, and decide which, if any, they should go and pick up.
		// Be careful, some objects might be guarded by monsters! 
		// You can see monsters by calling AIUtils.findCreaturesInRange(...).

        // Default behaviour: pick up nothing
        return null;
        
		// for(IGroundObject visibleGroundObjectContainer : allVisibleGroundObjects) {
		// 	IObject objectOnGround = visibleGroundObjectContainer.get();
			
		// 	if(objectOnGround.getObjectType() == ObjectType.ARMOUR) {
		// 		Armour a = (Armour)objectOnGround;
				
		// 		return visibleGroundObjectContainer;
				
		// 	} else if(objectOnGround.getObjectType() == ObjectType.WEAPON) {
		// 		Weapon w = (Weapon)objectOnGround;
				
		// 		return visibleGroundObjectContainer;
				
		// 	} else if(objectOnGround.getObjectType() == ObjectType.ITEM) {
		// 		DrinkableItem i = (DrinkableItem)objectOnGround;
				
		// 		return visibleGroundObjectContainer;
		// 	}
			
		// }
	}

    /** While your character is wandering around the world, it will see other monsters, which it may optionally attack.
	 *
	 * This method is called each tick with a list of all monsters currently on screen. If you wish to 
	 * attack one of them, return the creature object you wish to attack. 
	 **/
    public static ICreature shouldIAttackCreature(SimpleAI client, List<ICreature> visibleMonsters) {
		
		SelfState selfState = client.getSelfState();
		WorldState worldState = client.getWorldState();
		
		ICreature creatureToAttack = null;
        // Default behaviour: Attack nothing
		return null;
	}

    /**
	 * When your character has nothing else it do it (no items to pick up, or creatures to attack), it will call
	 * this method. The coordinate on the map you pick is the coordinate that the code will move to.
	 */
	public static Position whereShouldIGo(SimpleAI client) {

		SelfState selfState = client.getSelfState();
		WorldState worldState = client.getWorldState();
		
		IMap whatWeHaveSeenMap = worldState.getMap();
		
		int x1;
		int y1;
		int x2;
		int y2;

		boolean randomPositionInView = false; 
		if(randomPositionInView) {
			x1 = worldState.getViewXPos();
			y1 = worldState.getViewYPos();
			x2 = worldState.getViewXPos() + worldState.getViewWidth()-1;
			y2 = worldState.getViewYPos() + worldState.getViewHeight()-1;			
		} else {
			x1 = 0;
			y1 = 0;
			x2 = worldState.getWorldWidth();
			y2 = worldState.getWorldHeight();
		}
		
		// Default behaviour: Pick a random spot in the world and go there.
		Position p  = AIUtils.findRandomPositionOnMap(x1, y1, x2, y2, !randomPositionInView, whatWeHaveSeenMap);
		
		System.out.println("Going to "+p);
		if(p != null) {
			return p;
		}
		
		return null;
		
	}

    /** Each turn, we call this method to ask if you character should drink a potion (and which one it should drink). 
	 *
	 * To drink a potion, return the inventory object for the potion (ownable object). 
	 * To drink no potions this turn, return null. 
	 **/
	public static OwnableObject shouldIDrinkAPotion(SimpleAI client) {
		
		ICreature me = client.getSelfState().getPlayer();
		WorldState worldState = client.getWorldState();
		List<OwnableObject> ourInventory = me.getInventory();

		int percentHealthLeft = (int)(100d * (double)me.getHp() / (double)me.getMaxHp()); 

		// Default behaviour: if our health is less than 50, then drink the first potion 
		// in our inventory (but it might not be helpful in this situation!) 

		if(percentHealthLeft < 50) {
			
			for(OwnableObject oo : ourInventory) {
				IObject obj = oo.getContainedObject();
				if(obj.getObjectType() == ObjectType.ITEM) {
					DrinkableItem potion = (DrinkableItem)obj;
					return oo;
				}
			}			
		}
		
		// Otherwise drink no potions
		return null;
	}

    /** When your character picks up a new item (weapon or armour), they have a choice on whether or not to equip it.
	 * 
	 * Return true if you wish to put on or use this new item, or false otherwise.
	 * 
	 **/
	public static boolean shouldIEquipNewItem(SimpleAI client, IObject newItem) {
        // Default behavior: Equip any armor we pickup
		ICreature me = client.getSelfState().getPlayer();
		
		if(newItem.getObjectType() == ObjectType.ARMOUR) {
			Armour a = (Armour) newItem;			
			
			Armour previouslyEquipped = me.getArmour().get(a.getType());
			if(previouslyEquipped != null) {
				// Put your own logic here... compare what you have equipped with what you just picked up!
			}
			
			// Default behaviour: Always equip everything we pick up
			return true;
			
			
		} 
        // else if(newItem.getObjectType() == ObjectType.WEAPON) {
		// 	Weapon w = (Weapon) newItem;
			
		// 	Weapon previouslyEquipped = me.getWeapon();
		// 	if(previouslyEquipped != null) {
		// 		// Put your own logic here... compare what you have equipped with what you just picked up!
		// 	}

		// 	// Default behaviour: Always equip everything we pick up
		// 	return true;

		// }
		
		return false;
	}
    
    /** If a creature is attacking us (and we did not initiate the combat through the shouldIAttackCreature method), we 
	 * can choose whether to attack back or to ignore them. 
	 * 
	 * Your attacking an attacking creature back is not always the best course of action, 
	 * as some creatures will stop attacking once you leave their territory.
	 * 
	 * If you wish to attack back, return a creature from the list, otherwise return null.
	 **/
	public static ICreature unprovokedAttackShouldIAttackBack(SimpleAI client, List<ICreature> creaturesAttackingUs) {
		
		ICreature me = client.getSelfState().getPlayer();
		WorldState worldState = client.getWorldState();
		
		Collections.shuffle(creaturesAttackingUs);
        // Add logic, does not attack unprovoked by default
		return null;
	}
}
