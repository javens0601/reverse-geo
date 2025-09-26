package cn.javen.tool.reversegeo.model;

/**
 * @Description 空间索引
 * @Author: Javen
 * @CreateTime: 2025/9/23 15:33
 */
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.index.strtree.STRtree;
import java.util.List;

public class STRtreeIndex {
    private final STRtree tree = new STRtree();

    public void insert(Envelope envelope, Object item) {
        tree.insert(envelope, item);
    }

    public void build() {
        tree.build();
    }

    //public List<Object> query(Envelope envelope) {
    public List<Street> query(Envelope envelope) {
        return tree.query(envelope);
    }
}
