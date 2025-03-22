package org.zeroBzeroT.chatCo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ChatPlayer {
    public final Player player;
    public final UUID playerUUID;
    public boolean chatDisabled;
    public boolean tellsDisabled;
    public String LastMessenger;
    public String LastReceiver;
    private File IgnoreList;
    private List<String> ignores;

    public ChatPlayer(final Player p) throws IOException {
        player = p;
        playerUUID = p.getUniqueId();
        chatDisabled = false;
        tellsDisabled = false;
        LastMessenger = null;
        LastReceiver = null;
        ignores = new ArrayList<>();

        // Initialize ignore list file
        initializeIgnoreList();
    }

    // Private initialization method to avoid overridable method call in constructor
    private void initializeIgnoreList() throws IOException {
        File oldIgnores = new File(Main.dataFolder, "/ignorelists/" + this.player.getName() + ".txt");
        this.IgnoreList = new File(Main.dataFolder, "/ignorelists/" + this.playerUUID + ".txt");

        if (oldIgnores.exists()) {
            oldIgnores.renameTo(this.IgnoreList);
        }

        if (!this.IgnoreList.exists()) {
            this.IgnoreList.getParentFile().mkdir();
            this.IgnoreList.createNewFile();
        }

        // Initialize the ignores list
        updateIgnoreList();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void saveIgnoreList(final String p) throws IOException {
        if (!p.isEmpty()) {
            if (!this.isIgnored(p)) {
                try (FileWriter fwo = new FileWriter(this.IgnoreList, true);
                    BufferedWriter bwo = new BufferedWriter(fwo)) {
                    bwo.write(p);
                    bwo.newLine();
                }
            } else {
                this.ignores.remove(p);
                this.ignores.remove("");
                try (FileWriter fwo = new FileWriter(this.IgnoreList);
                    BufferedWriter bwo = new BufferedWriter(fwo)) {
                    for (final String print : this.ignores) {
                        bwo.write(print);
                        bwo.newLine();
                    }
                }
            }
        }

        this.updateIgnoreList();
    }

    public void unIgnoreAll() throws IOException {
        try (FileWriter fwo = new FileWriter(this.IgnoreList, false);
            BufferedWriter bwo = new BufferedWriter(fwo)) {
            bwo.flush();
        }

        this.updateIgnoreList();
    }

    public Player getLastMessenger() {
        if (this.LastMessenger != null) {
            return Bukkit.getPlayerExact(this.LastMessenger);
        }

        return null;
    }

    public void setLastMessenger(final Player sender) {
        this.LastMessenger = sender.getName();
    }

    public Player getLastReceiver() {
        if (this.LastReceiver != null) {
            return Bukkit.getPlayerExact(this.LastReceiver);
        }

        return null;
    }

    public void setLastReceiver(final Player sender) {
        this.LastReceiver = sender.getName();
    }

    private void updateIgnoreList() throws IOException {
        try (FileInputStream file = new FileInputStream(this.IgnoreList);
            InputStreamReader fileReader = new InputStreamReader(file);
            BufferedReader inIgnores = new BufferedReader(fileReader)) {

            String data = inIgnores.readLine();
            this.ignores = new ArrayList<>();

            while (data != null) {
                this.ignores.add(data);
                data = inIgnores.readLine();
            }
        }
    }

    public boolean isIgnored(final String p) {
        return this.ignores.contains(p);
    }

    public List<String> getIgnoreList() {
        return this.ignores;
    }
}
