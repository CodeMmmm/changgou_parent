package com.changgou.canal.listener;

import com.alibaba.fastjson.JSON;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.changgou.content.feign.ContentFeign;
import com.changgou.content.pojo.Content;
import com.xpand.starter.canal.annotation.*;
import entity.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

@CanalEventListener
public class CanalDataEventListener {

    @Autowired
    private ContentFeign contentFeign;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @ListenPoint(destination = "example",schema = "changgou_content",table = {"tb_content"},eventType = {CanalEntry.EventType.UPDATE,CanalEntry.EventType.INSERT,CanalEntry.EventType.DELETE})
    public void onEventUpdate(CanalEntry.EventType eventType,CanalEntry.RowData rowData){
        // 找到监听到的广告分类id
        String categoryId = getColumn(rowData,"category_id");
        System.out.println(categoryId);
        // 根据分类id查找出对应的广告
        Result<List<Content>> listResult = contentFeign.findByCategory(Long.valueOf(categoryId));
        // 将查找出的内容写入redis
        stringRedisTemplate.boundValueOps("content_"+categoryId).set(JSON.toJSONString(listResult.getData()));
    }

    public String getColumn(CanalEntry.RowData rowData,String columnName){
        // 增加和修改时会发生的分类id变化情况
        for (CanalEntry.Column column : rowData.getAfterColumnsList()) {
            if (columnName.equals(column.getName())){
                return column.getValue();
            }
        }

        // 删除时会发生的分类id变化情况
        for (CanalEntry.Column column : rowData.getBeforeColumnsList()) {
            if (columnName.equals(column.getName())){
                return column.getValue();
            }
        }
        return null;
    }


    /**
     * 新增监听
     * @param eventType
     * @param rowData
     */
    /*@InsertListenPoint
    public void onEventInsert(CanalEntry.EventType eventType,CanalEntry.RowData rowData){
        List<CanalEntry.Column> afterColumnsList = rowData.getAfterColumnsList();
        for (CanalEntry.Column column : afterColumnsList) {
            System.out.println("新增后的数据:" + column.getName() + "-------------" + column.getValue());
        }
    }*/

    /**
     * 修改监听
     * @param eventType
     * @param rowData
     */
    /*@UpdateListenPoint
    public void onEventUpdate(CanalEntry.EventType eventType,CanalEntry.RowData rowData){
        List<CanalEntry.Column> beforeColumnsList = rowData.getBeforeColumnsList();
        for (CanalEntry.Column column : beforeColumnsList) {
            System.out.println("修改前的数据:" + column.getName() + "-------------" + column.getValue());
        }

        List<CanalEntry.Column> afterColumnsList = rowData.getAfterColumnsList();
        for (CanalEntry.Column column : afterColumnsList) {
            System.out.println("修改后的数据:" + column.getName() + "-------------" + column.getValue());
        }
    }*/

    /**
     * 删除监听
     * @param eventType
     * @param rowData
     */
    /*@DeleteListenPoint
    public void onEventDelete(CanalEntry.EventType eventType,CanalEntry.RowData rowData){

        List<CanalEntry.Column> beforeColumnsList = rowData.getBeforeColumnsList();
        for (CanalEntry.Column column : beforeColumnsList) {
            System.out.println("删除前的数据:" + column.getName() + "-------------" + column.getValue());
        }
    }*/



}
