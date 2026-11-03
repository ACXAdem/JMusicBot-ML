package dev.cosgy.jmusicbot.util;


import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import java.io.PrintWriter;
import java.io.StringWriter;

public class StackTraceUtil {
    /** Sends the error details (stack trace) to the channel; truncates to Discord's 2000-character limit. */
    public static void sendStackTrace(MessageChannel channel, Throwable throwable) {
        if (channel == null || throwable == null) return;
        StringWriter writer = new StringWriter();
        PrintWriter pWriter = new PrintWriter(writer);
        throwable.printStackTrace(pWriter);
        pWriter.flush();
        String trace = writer.toString();
        int max = 2000 - 14; // room for the ``` marks and "..."
        if (trace.length() > max) trace = trace.substring(0, max) + "...";
        channel.sendMessage("```\n" + trace + "\n```").queue();
    }
}
