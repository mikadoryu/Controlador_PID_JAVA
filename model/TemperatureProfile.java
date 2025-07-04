package model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

public class TemperatureProfile {
    private TreeMap<Integer, Double> profile = new TreeMap<>();

    public void loadFromFile(String path) throws IOException {
        for (String line : Files.readAllLines(Path.of(path))) {
            String[] parts = line.split(",");
            int time = Integer.parseInt(parts[0].trim());
            double temp = Double.parseDouble(parts[1].trim());
            profile.put(time, temp);
        }
    }

    public double getSetpoint(int t) {
        Map.Entry<Integer, Double> floor = profile.floorEntry(t);
        Map.Entry<Integer, Double> ceil = profile.ceilingEntry(t);

        if (floor == null || ceil == null) return (floor != null) ? floor.getValue() : 0;

        if (floor.getKey().equals(ceil.getKey())) return floor.getValue();

        int t0 = floor.getKey();
        int t1 = ceil.getKey();
        double y0 = floor.getValue();
        double y1 = ceil.getValue();

        return y0 + (y1 - y0) * (t - t0) / (double) (t1 - t0);
    }

    public int getLastTime() {
        return profile.lastKey();
    }

}
