package ttps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import ben_and_asaf_ttp.thetownproject.shared_resources.Roles;

/**
 * {@code RoleBank} class is used to generate {@code Roles} and {@code Roles} lists
 * for {@code Game} instances
 * @author Ben Gilad and Asaf Yeshayahu
 * @version %I%
 * @see ben_and_asaf_ttp.thetownproject.shared_resources.Game
 * @see ben_and_asaf_ttp.thetownproject.shared_resources.Role
 * @since 1.0
 */
public class RolesBank {
	/**
	 * Return a random role (Not including DEAD or NONE)
	 * @param killer Whether to randomize a killer or not
	 * @return the randomized role
	 */
	private static Roles random(final boolean killer){
		List<Roles> roles = new ArrayList<Roles>();
		
		roles.add(Roles.CITIZEN);
		if(killer){
			roles.add(Roles.KILLER);
		}
		Collections.shuffle(roles);
		return roles.get(new Random().nextInt(roles.size() - 1));
	}
	
	/**
	 * Returns an {@code ArrayList} of roles based on how many roles are requested
	 * and one can choose between 5, 8 or 10 anything else will return null
	 * @param roleBankSize how many roles are requested (5,8 or 10)
	 * @return the {@code List} with the roles
	 */
	public static ArrayList<Roles> getRoleBank(final int roleBankSize){
		ArrayList list = null;
		switch(roleBankSize){
		case 5:
			list = new ArrayList<Roles>();
			list.add(Roles.CITIZEN);
			list.add(Roles.CITIZEN);
			list.add(Roles.HEALER);
			list.add(Roles.SNITCH);
			list.add(Roles.KILLER);
			break;
		case 8:
			list = new ArrayList<Roles>();
			list.add(Roles.CITIZEN);
			list.add(Roles.CITIZEN);
			list.add(Roles.CITIZEN);
			list.add(Roles.HEALER);
			list.add(Roles.SNITCH);
			list.add(RolesBank.random(false));
			list.add(Roles.KILLER);
			list.add(Roles.KILLER);
			break;
		case 10:
			list = new ArrayList<Roles>();
			list.add(Roles.CITIZEN);
			list.add(Roles.CITIZEN);
			list.add(Roles.CITIZEN);
			list.add(Roles.CITIZEN);
			list.add(Roles.HEALER);
			list.add(Roles.SNITCH);
			list.add(RolesBank.random(true));
			list.add(RolesBank.random(true));
			list.add(Roles.KILLER);
			list.add(Roles.KILLER);
			break;
		default:
			return null;
		}
		
		return list;
	}
}
