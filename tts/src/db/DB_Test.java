package db;

import ben_and_asaf_ttp.thetownproject.shared_resources.Player;

class DB_Test {
	public static void main(final String[] args) {
		DB db = DB.getDB();
		db.getEm().getTransaction().begin();
		Player p1 = new Player("Asaf", "1");
		Player p2 = new Player("Ben", "1");
		Player p3 = new Player("Lior", "1");
		Player p4 = new Player("Shayke", "1");
		Player p5 = new Player("Yosef", "1");
		
		db.getEm().persist(p1);
		db.getEm().persist(p2);
		db.getEm().persist(p3);
		db.getEm().persist(p4);
		db.getEm().persist(p5);
		db.getEm().getTransaction().commit();
	}
}
