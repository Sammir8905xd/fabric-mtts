package com.orangy.mtts;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.List;

/**
 * Mod de Megafonía TTS para MTR.
 * Reproduce voz real sin usar el narrador de Minecraft.
 * Creado por OranGy para la versión 1.20.4.
 */
public class MttsMod implements ModInitializer {

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("mtts")
                .then(CommandManager.argument("mensaje", StringArgumentType.greedyString())
                .executes(context -> {
                    String mensaje = StringArgumentType.getString(context, "mensaje");
                    ServerPlayerEntity sourcePlayer = context.getSource().getPlayer();

                    if (sourcePlayer != null) {
                        double radio = 30.0;
                        List<ServerPlayerEntity> jugadoresCerca = sourcePlayer.getServerWorld().getPlayers(player -> 
                            player.squaredDistanceTo(sourcePlayer) < radio * radio
                        );

                        // Enviamos el texto visual al chat para todos los del área
                        for (ServerPlayerEntity player : jugadoresCerca) {
                            player.sendMessage(Text.literal("§6[Megafonía] §f" + mensaje), false);
                        }

                        // Ejecutamos la voz en un hilo separado para no congelar el servidor/juego
                        new Thread(() -> {
                            try {
                                speak(mensaje);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }).start();
                    }
                    return 1;
                })));
        });
    }

    /**
     * Lógica para hablar usando el motor de voz del Sistema Operativo (Windows PowerShell)
     * Esto evita usar el narrador interno de Minecraft.
     */
    private void speak(String text) {
        try {
            // Usamos un comando de PowerShell para ejecutar el sintetizador de voz nativo de Windows
            // Es la forma más limpia sin añadir archivos .jar externos pesados
            String command = "Add-Type -AssemblyName System.Speech; " +
                             "$speak = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                             "$speak.Speak('" + text.replace("'", "") + "')";
            
            ProcessBuilder pb = new ProcessBuilder("powershell", "-Command", command);
            pb.start();
        } catch (Exception e) {
            System.err.println("Error al reproducir TTS: " + e.getMessage());
        }
    }
}
