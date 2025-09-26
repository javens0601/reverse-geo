package cn.javen.tool.reversegeo.service;

import cn.javen.tool.reversegeo.model.STRtreeIndex;
import cn.javen.tool.reversegeo.model.Street;
import com.opencsv.CSVReader;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKTReader;
import org.springframework.stereotype.Service;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * @Description 加载数据 构建空间索引
 * @Author: Javen
 * @CreateTime: 2025/9/23 15:29
 */
@Service
public class GeoDataLoader {

    // GeometryFactory 用于创建几何对象（如点、线、多边形等）
    // PrecisionModel 指定坐标的精度模型，这里使用默认的浮点精度
    // SRID 4326 表示 WGS84 坐标系，即经纬度坐标系统
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    private final WKTReader wktReader = new WKTReader(geometryFactory);

    // 存储所有街道（可替换为 RTree 索引）
    private final List<Street> streets = new ArrayList<>();

    // 空间索引：使用 STRtree（JTS 提供）
    private final STRtreeIndex strTreeIndex = new STRtreeIndex();

    // 行政区划映射（示例）
    private final Map<String, String> districtMap = new HashMap<>();
    private final Map<String, String> cityMap = new HashMap<>();
    private final Map<String, String> provinceMap = new HashMap<>();

    public GeoDataLoader() {
        loadAdminMapping();       // 加载省市区映射
        loadStreetsFromCSV();     // 加载街道数据
        buildSpatialIndex();      // 构建空间索引
    }

    /**
     * 查询省
     * SELECT left(t.administrative_code, 2), t.area_name from t_pbm_poi_districts t where t.level=2 and t.is_deleted=0 and t.parent_id=1 order by t.administrative_code;
     *
     * 查询市
     * SELECT left(t.administrative_code, 4) as code, t.area_name from t_pbm_poi_districts t where t.level=3
     * and t.county_id=0
     * and t.whole_id like '1,%'
     * and t.is_deleted=0 having code != '' order by t.administrative_code;
     *
     *
     */
    private void loadAdminMapping() {
        try {
            List<String> provices = Files.readAllLines(Path.of("src/main/resources/province.txt"));
            provices.forEach(s -> {
                String[] split = s.split(",");
                provinceMap.put(split[0], split[1]);
            });

            List<String> citys = Files.readAllLines(Path.of("src/main/resources/city.txt"));
            citys.forEach(s -> {
                String[] split = s.split(",");
                cityMap.put(split[0], split[1]);
            });

            List<String> districts = Files.readAllLines(Path.of("src/main/resources/district.txt"));
            districts.forEach(s -> {
                String[] split = s.split(",");
                districtMap.put(split[0], split[1]);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadStreetsFromCSV() {
        String csvFile = "src/main/resources/streets_over.csv"; // 放在 resources 目录
        try (Reader reader = new InputStreamReader(new FileInputStream(csvFile), StandardCharsets.UTF_8);
             CSVReader csvReader = new CSVReader(reader)) {

            String[] line;
            boolean first = true;
            while ((line = csvReader.readNext()) != null) {
                if (first) {
                    first = false;
                    continue;
                } // skip header

                Street street = new Street();
                street.setStreetId(line[0]);
                street.setName(line[1]);
                street.setLevel(line[2]);

                // 解析 center (POINT)
                if (line[3] != null && !line[3].isEmpty()) {
                    String[] center = parsePointFromWKT(line[3]);
                    if (center != null) {
                        double lng = Double.parseDouble(center[0]);
                        double lat = Double.parseDouble(center[1]);
                        street.setCenter(geometryFactory.createPoint(new Coordinate(lng, lat)));
                    }
                }

                // 解析 polygon (WKT)
                if (line[4] != null && !line[4].isEmpty()) {
                    Geometry geom = wktReader.read(line[4]);
                    street.setPolygon(geom);
                }

                streets.add(street);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String[] parsePointFromWKT(String wkt) {
        // POINT(116.4056 39.9126)
        int start = wkt.indexOf('(') + 1;
        int end = wkt.indexOf(')');
        if (start > 0 && end > start) {
            String[] coords = wkt.substring(start, end).split(" ");
            return new String[]{coords[0], coords[1]};
        }
        return null;
    }

    /**
     * 构建空间索引（STRtree）以提高逆地理编码的查询效率
     * 遍历所有已加载的街道数据，将具有有效多边形的街道插入到STRtree空间索引中
     * 每个街道的多边形外包矩形（Envelope）作为索引键，街道对象本身作为值
     * 最后调用build()方法完成索引的构建
     */
    private void buildSpatialIndex() {
        // 遍历所有街道对象
        for (Street street : streets) {
            // 检查街道是否具有有效的多边形几何对象
            if (street.getPolygon() != null) {
                // 将街道的多边形外包矩形和街道对象插入到STRtree索引中
                // getEnvelopeInternal()获取几何对象的最小外包矩形，用于空间索引的快速筛选
                strTreeIndex.insert(street.getPolygon().getEnvelopeInternal(), street);
            }
        }
        // 构建STRtree索引，使其可以被高效查询
        strTreeIndex.build();
    }

    /**
     * 根据给定的经纬度进行逆地理编码，返回对应的街道信息
     * @param lng 经度
     * @param lat 纬度
     * @return 匹配的Street对象，如果未找到则返回null
     */
    public Street reverseGeocode(double lng, double lat) {
        // 使用给定的经纬度创建一个点几何对象
        Point point = geometryFactory.createPoint(new Coordinate(lng, lat));
        
        // 使用STRtree空间索引查询可能包含该点的候选街道（基于外包矩形）
        // 使用点的外包矩形（Envelope）作为查询条件，在STRtree空间索引中查找可能包含该点的候选街道
        // getEnvelopeInternal() 获取几何对象（此处为点）的最小外包矩形，用于空间索引的快速筛选
        List<Street> candidates = strTreeIndex.query(point.getEnvelopeInternal());

        // 遍历候选街道，精确判断点是否在街道的多边形内部
        for (Street street : candidates) {
            if (street.getPolygon().contains(point)) {
                // 找到包含该点的街道，立即返回
                return street;
            }
        }
        
        // 如果没有找到匹配的街道，返回null
        return null;
    }

    public Map<String, String> getAddress(String streetId) {
        Map<String, String> addr = new HashMap<>();
        String code6 = streetId.length() >= 6 ? streetId.substring(0, 6) : "";
        String code4 = streetId.length() >= 4 ? streetId.substring(0, 4) : "";
        String code2 = streetId.length() >= 2 ? streetId.substring(0, 2) : "";

        addr.put("province", provinceMap.getOrDefault(code2, "未知省"));
        addr.put("city", cityMap.getOrDefault(code4, "未知市"));
        addr.put("district", districtMap.getOrDefault(code6, "未知区"));
        return addr;
    }
}