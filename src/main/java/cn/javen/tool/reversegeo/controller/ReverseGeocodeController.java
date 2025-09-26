package cn.javen.tool.reversegeo.controller;

/**
 * @Description
 * @Author: Javen
 * @CreateTime: 2025/9/23 15:35
 */

import cn.javen.tool.reversegeo.CsvUtil;
import cn.javen.tool.reversegeo.service.GeoDataLoader;
import com.opencsv.bean.CsvConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/geocode")
public class ReverseGeocodeController {

    @Autowired
    private GeoDataLoader geoDataLoader;

    @GetMapping("/reverse")
    public ResponseEntity<?> reverse(
            @RequestParam double lng,
            @RequestParam double lat) {

        var street = geoDataLoader.reverseGeocode(lng, lat);
        if (street == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, String> addr = geoDataLoader.getAddress(street.getStreetId());
        addr.put("street", street.getName());
        addr.put("streetid", street.getStreetId());

        return ResponseEntity.ok(addr);
    }

    @PostMapping("/batch-reverse")
    public ResponseEntity<?> batchReverse(@RequestBody List<String> locationList) {
        if (locationList.size() > 100) {
            return ResponseEntity.ok("每批次查询不能超过100个坐标点");
        }

        Set<Map<String, String>> collect = locationList.parallelStream().map(loc -> {
                    String[] split = loc.split(",");
                    double lng = Double.parseDouble(split[0]);
                    double lat = Double.parseDouble(split[1]);
                    var street = geoDataLoader.reverseGeocode(lng, lat);
                    if (Objects.nonNull(street)) {
                        Map<String, String> addr = geoDataLoader.getAddress(street.getStreetId());
                        addr.put("street", street.getName());
                        addr.put("streetid", street.getStreetId());
                        addr.put("location", loc);
                        return addr;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return ResponseEntity.ok(collect);
    }

    @PostMapping("/reverse")
    public ResponseEntity<?> reversePost(@RequestBody Coordinate coord) {
        return reverse(coord.lng, coord.lat);
    }

    static class Coordinate {
        double lng, lat;

        // getter/setter
        public double getLng() {
            return lng;
        }

        public void setLng(double lng) {
            this.lng = lng;
        }

        public double getLat() {
            return lat;
        }

        public void setLat(double lat) {
            this.lat = lat;
        }
    }

    @GetMapping("/csv-reverse")
    public ResponseEntity<?> csvReverse(@RequestParam String name) throws IOException {
        String filename = name;

        List<String[]> sourceLines = CsvUtil.read(filename);
        List<String[]> newLines = sourceLines.stream().map(line -> {
            double lng = 0;
            try {
                lng = Double.parseDouble(line[2]);
                double lat = Double.parseDouble(line[3]);
                var street = geoDataLoader.reverseGeocode(lng, lat);
                if (Objects.nonNull(street)) {
                    Map<String, String> addr = geoDataLoader.getAddress(street.getStreetId());
                    addr.put("street", street.getName());
                    addr.put("streetid", street.getStreetId());

                    List<String> newLine = List.of(line[0], line[1], line[2], line[3],
                            addr.getOrDefault("province", ""),
                            addr.getOrDefault("city", ""),
                            addr.getOrDefault("district", ""),
                            addr.getOrDefault("street", ""));
                    return newLine.stream().toArray(String[]::new);
                }
                return line;
            }
            catch (NumberFormatException e) {
                // 表头行
                return List.of(line[0], line[1], line[2], line[3], "省", "市", "区", "街道").stream().toArray(String[]::new);
            }
        }).collect(Collectors.toUnmodifiableList());

        String fileoutputname = filename.substring(0, filename.length() - 4) + "-ok.csv";
        CsvUtil.write(fileoutputname, newLines);

        return ResponseEntity.ok("finish");
    }
}