package com.changgou.goods.dao;
import com.changgou.goods.pojo.Brand;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.common.Mapper;

/****
 * @Author:shenkunlin
 * @Description:Brand的Dao
 * @Date 2019/6/14 0:12
 *****/
public interface BrandMapper extends Mapper<Brand> {
    /**
     * 根据分类id查找品牌
     * @param id
     */
    @Select("SELECT tb.* FROM tb_category_brand tcb,tb_brand tb WHERE  tcb.`brand_id` = tb.`id` AND tcb.`category_id` = #{id}")
    void findByCategoryId(Integer id);
}
