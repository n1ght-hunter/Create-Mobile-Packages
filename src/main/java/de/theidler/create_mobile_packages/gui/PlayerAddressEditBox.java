package de.theidler.create_mobile_packages.gui;

import com.simibubi.create.content.logistics.AddressEditBox;
import com.simibubi.create.content.trains.schedule.DestinationSuggestions;
import net.createmod.catnip.data.IntAttached;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An AddressEditBox that adds all online player names to the autocomplete suggestions.
 * Players are suggested with the "@PlayerName" format used by the mod's addressing system.
 */
public class PlayerAddressEditBox extends AddressEditBox {

    public PlayerAddressEditBox(Screen screen, Font pFont, int pX, int pY,
                                int pWidth, int pHeight, boolean anchorToBottom, String localAddress) {
        super(screen, pFont, pX, pY, pWidth, pHeight, anchorToBottom, localAddress);
        addOnlinePlayerSuggestions(localAddress);
    }

    @SuppressWarnings("unchecked")
    private void addOnlinePlayerSuggestions(String localAddress) {
        try {
            // Get the destinationSuggestions field from AddressEditBox
            Field destinationSuggestionsField = AddressEditBox.class.getDeclaredField("destinationSuggestions");
            destinationSuggestionsField.setAccessible(true);
            DestinationSuggestions destinationSuggestions = (DestinationSuggestions) destinationSuggestionsField.get(this);

            // Get the viableStations field from DestinationSuggestions
            Field viableStationsField = DestinationSuggestions.class.getDeclaredField("viableStations");
            viableStationsField.setAccessible(true);
            List<IntAttached<String>> viableStations = (List<IntAttached<String>>) viableStationsField.get(destinationSuggestions);

            // Build a set of already-added addresses to avoid duplicates
            Set<String> alreadyAdded = new HashSet<>();
            for (IntAttached<String> station : viableStations) {
                alreadyAdded.add(station.getValue());
            }

            // Add all online players
            ClientPacketListener connection = Minecraft.getInstance().getConnection();
            if (connection != null) {
                for (PlayerInfo playerInfo : connection.getOnlinePlayers()) {
                    String playerName = "@" + playerInfo.getProfile().getName();
                    if (!alreadyAdded.contains(playerName)) {
                        viableStations.add(IntAttached.withZero(playerName));
                        alreadyAdded.add(playerName);
                    }
                }
            }

            // Refresh suggestions to pick up the new entries
            destinationSuggestions.updateCommandInfo();

        } catch (NoSuchFieldException | IllegalAccessException e) {
            // Log error but don't crash - autocomplete will just work without player suggestions
            e.printStackTrace();
        }
    }
}
