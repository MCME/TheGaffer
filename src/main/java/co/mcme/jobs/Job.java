package co.mcme.jobs;

import java.util.ArrayList;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class Job {

    private Player admin;
    private String name;
    private boolean status;
    private Location warpto;
    private ArrayList<String> runners = new ArrayList();

    public Job(String n, Player a, boolean s) {
        this.admin = a;
        this.name = n;
        this.status = s;
    }
}
