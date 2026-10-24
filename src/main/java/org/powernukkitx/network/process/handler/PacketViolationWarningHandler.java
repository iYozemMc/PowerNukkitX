package org.powernukkitx.network.process.handler;

import org.powernukkitx.Player;
import org.powernukkitx.Server;
import org.powernukkitx.network.process.PacketHandler;
import org.powernukkitx.network.process.PlayerSessionHolder;
import lombok.extern.slf4j.Slf4j;
import org.cloudburstmc.protocol.bedrock.packet.PacketViolationWarningPacket;

import javax.annotation.Nullable;

/**
 * @author Kaooot
 */
@Slf4j
public class PacketViolationWarningHandler implements PacketHandler<PacketViolationWarningPacket> {

    @Override
    public boolean requiresPlayer() {
        return false;
    }

    @Override
    public void handle(PacketViolationWarningPacket packet, PlayerSessionHolder holder, Server server) {
        final Player player = holder.getPlayer();
        log.warn("Violation warning from {}: {} {} on packet id {} ({})",
                player != null ? player.getName() : holder.getSession().getSocketAddress(), packet.getViolationSeverity(),
                packet.getViolationType(), packet.getViolatingPacketId(),
                describeContext(packet.getViolationContext()));
    }

     /**
     * The context is free text of the client's choosing, and this handler runs before the session
     * has authenticated, so it is cut to one log line's worth and stripped of the line breaks that
     * would let it forge log entries of its own.
     *
     * @param context the context as the client sent it
     * @return what to put in the log line
     */
    static String describeContext(@Nullable String context){
        if (context == null || context.isEmpty()){
            return "no context";
        }
        final String shortened = context.length() > 256
            ? context.substring(0, 256) + "..."
            : context;
        return shortened.replace('\r', ' ').replace('\n', ' ');
    }
}
