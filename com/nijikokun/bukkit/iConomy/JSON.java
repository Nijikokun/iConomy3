package com.nijikokun.bukkit.iConomy;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * iConomy v1.x
 * Copyright (C) 2010  Nijikokun <nijikokun@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
/**
 * JSON
 *
 * Controls the log files, not an extensive JSON parser or anything. Simple quote exchange.
 *
 * @author Nijiko
 */
public class JSON {

    private FileManager file;
    private boolean log;

    public JSON(String directory, String filename, boolean log) {
        file = new FileManager(directory, filename, log);
        this.log = log;
    }

    /*
     * Gives the current date formatted nice and neat.
     */
    public static String date() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date now = new Date();
        return format.format(now);
    }

    /*
     * Parses data to JSON format
     *
     * @param data The data to be written.
     */
    public String parse(String input) {
        return input.replace('\'', '"');
    }

    /*
     * Writes the data given out to the log file.
     *
     * @param data The data to be written.
     */
    public void write(String data) {
        if (log) {
            try {
                file.existsCreate();
                file.append(date() + "|" + parse(data));
            } catch (Exception es) {
                System.out.println("[Logging] " + es.getMessage());
            }
        }
    }
}
