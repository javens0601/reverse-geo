package cn.javen.tool.reversegeo.model;

/**
 * @Description
 * @Author: Javen
 * @CreateTime: 2025/9/23 15:28
 */
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

public class Street {
    private String streetId;
    private String name;
    private String level;
    private Point center;
    private Geometry polygon;

    // Getters and Setters
    public String getStreetId() { return streetId; }
    public void setStreetId(String streetId) { this.streetId = streetId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public Point getCenter() { return center; }
    public void setCenter(Point center) { this.center = center; }

    public Geometry getPolygon() { return polygon; }
    public void setPolygon(Geometry polygon) { this.polygon = polygon; }
}