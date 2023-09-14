package cn.electric.middleware.db.router;

/**
 * 数据源基础配置
 *
 * @author alex5leo7
 */
public class DBRouterBase {

    private String tbIdx;

    public String getTbIdx() {
        return DBContextHolder.getTBKey();
    }

}
