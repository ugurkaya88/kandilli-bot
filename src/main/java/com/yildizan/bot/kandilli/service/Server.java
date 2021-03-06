package com.yildizan.bot.kandilli.service;

import com.yildizan.bot.kandilli.model.Earthquake;
import javafx.util.Pair;
import org.jsoup.Jsoup;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Server {

    private static final String url = "http://www.koeri.boun.edu.tr/scripts/lst0.asp";

    // archive html to reduce request count
    private static String html = "";
    private static long timestamp = System.currentTimeMillis();

    private Server() {}

    public static Earthquake last() {
        try {
            return extract(fetch()).getKey();
        }
        catch (Exception e) {
            return null;
        }
    }

    public static List<Earthquake> last(int count) {
        List<Earthquake> earthquakes = new ArrayList<>();
        String text = fetch();
        for(int i = 0; i < count; i++) {
            Pair<Earthquake, String> pair = extract(text);
            earthquakes.add(pair.getKey());
            text = pair.getValue();
        }
        return earthquakes;
    }

    public static Earthquake lastGreater(double threshold) {
        String text = fetch();
        while(text.length() > 0) {
            Pair<Earthquake, String> pair = extract(text);
            if(pair.getKey() == null) {
                break;
            }
            else if(pair.getKey().getMagnitude() >= threshold) {
                return pair.getKey();
            }
            text = pair.getValue();
        }
        return null;
    }

    public static List<Earthquake> lastGreater(double threshold, int count) {
        List<Earthquake> earthquakes = new ArrayList<>();
        String text = fetch();
        while(text.length() > 0 && earthquakes.size() < count) {
            Pair<Earthquake, String> pair = extract(text);
            if(pair.getKey() == null) {
                break;
            }
            else if(pair.getKey().getMagnitude() >= threshold) {
                earthquakes.add(pair.getKey());
            }
            text = pair.getValue();
        }
        return earthquakes;
    }

    public static List<Earthquake> lastIn(int minutes) {
        List<Earthquake> earthquakes = new ArrayList<>();
        String text = fetch();
        while(text.length() > 0) {
            Pair<Earthquake, String> pair = extract(text);
            Earthquake earthquake = pair.getKey();
            long now = System.currentTimeMillis();
            long duration = TimeUnit.MILLISECONDS.convert(minutes, TimeUnit.MINUTES);
            if(now - earthquake.getDate().getTime() < duration) {
                earthquakes.add(earthquake);
            }
            else {
                break;
            }
            text = pair.getValue();
        }
        return earthquakes;
    }

    private static String fetch() {
        try {
            // check if archive older than 1 minute
            if(System.currentTimeMillis() - timestamp  > 60000 || html.isEmpty()) {
                html = Jsoup
                    .connect(url)
                    .get()
                    .selectFirst("pre")
                    .html();
                timestamp = System.currentTimeMillis();
            }
            int beginning = findBeginning(html);
            return html.substring(beginning);
        }
        catch (Exception e) {
            return "";
        }
    }

    private static Pair<Earthquake, String> extract(String text) {
        String remaining = "";
        try {
            int index = text.indexOf("\n");
            String[] tokens = text.substring(0, index).split("\\s+");
            remaining = text.substring(index + 1);

            Earthquake earthquake = new Earthquake();

            SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
            earthquake.setDate(format.parse(tokens[0] + ' ' + tokens[1]));
            earthquake.setLatitude(Double.parseDouble(tokens[2]));
            earthquake.setLongitude(Double.parseDouble(tokens[3]));
            earthquake.setDepth(Double.parseDouble(tokens[4]));
            earthquake.setMagnitude(Double.parseDouble(tokens[6]));

            StringBuilder location = new StringBuilder();
            for(int i = 8; i < tokens.length - 1; i++) {
                location.append(tokens[i]).append(' ');
            }
            earthquake.setLocation(location.toString());

            return new Pair<>(earthquake, remaining);
        }
        catch (ParseException e) {
            return new Pair<>(null, remaining);
        }
        catch (Exception e) {
            return new Pair<>(null, "");
        }
    }

    private static int findBeginning(String text) {
        int index = 0;
        for(int i = 0; i < 6; i++) {
            index = text.indexOf("\n", index + 1);
        }
        return index + 1;
    }
}
