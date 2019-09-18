package com.changgou.goods.service.impl;
import com.alibaba.fastjson.JSON;
import com.changgou.goods.dao.BrandMapper;
import com.changgou.goods.dao.CategoryMapper;
import com.changgou.goods.dao.SkuMapper;
import com.changgou.goods.dao.SpuMapper;
import com.changgou.goods.pojo.Goods;
import com.changgou.goods.pojo.Sku;
import com.changgou.goods.pojo.Spu;
import com.changgou.goods.service.SpuService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import entity.IdWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/****
 * @Author:shenkunlin
 * @Description:Spu业务层接口实现类
 * @Date 2019/6/14 0:16
 *****/
@Service
public class SpuServiceImpl implements SpuService {

    @Autowired
    private SpuMapper spuMapper;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private BrandMapper brandMapper;

    /**
     * 还原被删除商品
     * @param spuId
     */
    @Override
    public void restore(Long spuId) {
        Spu spu = spuMapper.selectByPrimaryKey(spuId);
        if ("0".equals(spu.getIsDelete())){
            throw new RuntimeException("无法还原未删除商品");
        }
        spu.setIsDelete("0");
        spu.setStatus("0");
        spuMapper.updateByPrimaryKeySelective(spu);
    }

    /***
     * 逻辑删除
     * @param spuId
     */
    @Override
    public void logicDelete(Long spuId) {
        // 修改is_delete，删除前先下架
        Spu spu = spuMapper.selectByPrimaryKey(spuId);
        if ("1".equals(spu.getIsMarketable())){
            throw new RuntimeException("不能删除已上架的商品");
        }
        spu.setIsDelete("1");
        spu.setStatus("0");
        spuMapper.updateByPrimaryKeySelective(spu);
    }

    /***
     * 批量下架
     * @param ids:需要下架的商品ID集合
     * @return
     */
    @Override
    public int pullMany(Long[] ids) {
        Spu spu = new Spu();
        spu.setIsMarketable("0");
        Example example = new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        // 筛选未删除，已上架的
        criteria.andIn("id",Arrays.asList(ids));
        criteria.andEqualTo("isDelete","0");
        criteria.andEqualTo("isMarketable","1");
        return spuMapper.updateByExampleSelective(spu,example);
    }

    /***
     * 批量上架
     * @param ids:需要上架的商品ID集合
     * @return
     */
    @Override
    public int putMany(Long[] ids) {
        Spu spu = new Spu();
        spu.setIsMarketable("1");
        Example example = new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        // 筛选未删除，已审核，未上架的
        criteria.andIn("id", Arrays.asList(ids));
        criteria.andEqualTo("isDelete","0");
        criteria.andEqualTo("status","1");
        criteria.andEqualTo("isMarketable","0");
        return spuMapper.updateByExampleSelective(spu,example);
    }

    /**
     * 商品上架
     * @param spuId
     */
    @Override
    public void put(Long spuId) {
        Spu spu = spuMapper.selectByPrimaryKey(spuId);
        // 是否删除
        if ("1".equals(spu.getIsDelete())){
            throw new RuntimeException("error 商品已删除");
        }
        // 是否通过审核
        if ("0".equals(spu.getStatus())){
            throw new RuntimeException("error 商品未审核");
        }
        // 商家
        spu.setIsMarketable("1");
        spuMapper.updateByPrimaryKeySelective(spu);
    }

    /***
     * 商品下架
     * @param spuId
     */
    @Override
    public void pull(Long spuId) {
        Spu spu = spuMapper.selectByPrimaryKey(spuId);
        // 是否删除
        if ("1".equals(spu.getIsDelete())){
            throw new RuntimeException("error 商品已删除");
        }
        // 下架
        spu.setIsMarketable("0");
        spuMapper.updateByPrimaryKeySelective(spu);
    }

    /***
     * 商品审核
     * @param spuId
     */
    @Override
    public void audit(Long spuId) {
        Spu spu = spuMapper.selectByPrimaryKey(spuId);
        // 是否删除
        if (spu.getIsDelete().equals("1")){
            throw new RuntimeException("error，该商品已删除");
        }
        spu.setStatus("1");
        // 自动上架
        spu.setIsMarketable("1");
        spuMapper.updateByPrimaryKeySelective(spu);
    }

    /**
     * 根据SPU的ID查找SPU以及对应的SKU集合
     * @param spuId
     * @return
     */
    @Override
    public Goods findGoodsById(Long spuId) {
        Goods goods = new Goods();
        // 查找spu
        Spu spu = spuMapper.selectByPrimaryKey(spuId);
        goods.setSpu(spu);
        // 查找skulist
        Sku sku = new Sku();
        sku.setSpuId(Long.valueOf(String.valueOf(spuId)));
        List<Sku> skuList = skuMapper.select(sku);
        goods.setSkuList(skuList);
        return goods;
    }

    /**
     * 保存商品
     * @param goods
     */
    @Override
    public void saveGoods(Goods goods) {
        // 保存spu
        Spu spu = goods.getSpu();
        if (spu.getId() == null){
            spu.setId(idWorker.nextId());
            spuMapper.insertSelective(spu);
        }else {
            // 修改spu
            spuMapper.updateByPrimaryKey(spu);
            // 删除之前的所有sku
            Sku sku = new Sku();
            sku.setSpuId(Long.valueOf(String.valueOf(spu.getId())));
            skuMapper.delete(sku);
        }
        // 保存 sku集合
        List<Sku> skuList = goods.getSkuList();
        Date date = new Date();
        for (Sku sku : skuList) {
            // 生成唯一id
            sku.setId(Long.valueOf(String.valueOf(idWorker.nextId())));
            // 生成名字 spu.name + sku.spec
            String name = spu.getName();
            // 防止空指针
            if (StringUtils.isEmpty(sku.getSpec())){
                sku.setSpec("{}");
            }
            Map<String,String> specMap = JSON.parseObject(sku.getSpec(),Map.class);
            for (Map.Entry<String, String> entry : specMap.entrySet()) {
                name += " " + entry.getValue();
            }
            sku.setName(name);
            // 设置创建时间
            sku.setCreateTime(date);
            // 设置更新时间
            sku.setUpdateTime(date);
            // 设置spu类id
            sku.setSpuId(Long.valueOf(String.valueOf(spu.getId())));
            // 设置分类id
            sku.setCategoryId(spu.getCategory3Id());
            // 设置分类名称
            sku.setCategoryName(categoryMapper.selectByPrimaryKey(spu.getCategory3Id()).getName());
            // 设置品牌名称
            sku.setBrandName(brandMapper.selectByPrimaryKey(spu.getBrandId()).getName());
            // 新增sku
            skuMapper.insertSelective(sku);
        }
    }

    /**
     * Spu条件+分页查询
     * @param spu 查询条件
     * @param page 页码
     * @param size 页大小
     * @return 分页结果
     */
    @Override
    public PageInfo<Spu> findPage(Spu spu, int page, int size){
        //分页
        PageHelper.startPage(page,size);
        //搜索条件构建
        Example example = createExample(spu);
        //执行搜索
        return new PageInfo<Spu>(spuMapper.selectByExample(example));
    }

    /**
     * Spu分页查询
     * @param page
     * @param size
     * @return
     */
    @Override
    public PageInfo<Spu> findPage(int page, int size){
        //静态分页
        PageHelper.startPage(page,size);
        //分页查询
        return new PageInfo<Spu>(spuMapper.selectAll());
    }

    /**
     * Spu条件查询
     * @param spu
     * @return
     */
    @Override
    public List<Spu> findList(Spu spu){
        //构建查询条件
        Example example = createExample(spu);
        //根据构建的条件查询数据
        return spuMapper.selectByExample(example);
    }


    /**
     * Spu构建查询对象
     * @param spu
     * @return
     */
    public Example createExample(Spu spu){
        Example example=new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        if(spu!=null){
            // 主键
            if(!StringUtils.isEmpty(spu.getId())){
                    criteria.andEqualTo("id",spu.getId());
            }
            // 货号
            if(!StringUtils.isEmpty(spu.getSn())){
                    criteria.andEqualTo("sn",spu.getSn());
            }
            // SPU名
            if(!StringUtils.isEmpty(spu.getName())){
                    criteria.andLike("name","%"+spu.getName()+"%");
            }
            // 副标题
            if(!StringUtils.isEmpty(spu.getCaption())){
                    criteria.andEqualTo("caption",spu.getCaption());
            }
            // 品牌ID
            if(!StringUtils.isEmpty(spu.getBrandId())){
                    criteria.andEqualTo("brandId",spu.getBrandId());
            }
            // 一级分类
            if(!StringUtils.isEmpty(spu.getCategory1Id())){
                    criteria.andEqualTo("category1Id",spu.getCategory1Id());
            }
            // 二级分类
            if(!StringUtils.isEmpty(spu.getCategory2Id())){
                    criteria.andEqualTo("category2Id",spu.getCategory2Id());
            }
            // 三级分类
            if(!StringUtils.isEmpty(spu.getCategory3Id())){
                    criteria.andEqualTo("category3Id",spu.getCategory3Id());
            }
            // 模板ID
            if(!StringUtils.isEmpty(spu.getTemplateId())){
                    criteria.andEqualTo("templateId",spu.getTemplateId());
            }
            // 运费模板id
            if(!StringUtils.isEmpty(spu.getFreightId())){
                    criteria.andEqualTo("freightId",spu.getFreightId());
            }
            // 图片
            if(!StringUtils.isEmpty(spu.getImage())){
                    criteria.andEqualTo("image",spu.getImage());
            }
            // 图片列表
            if(!StringUtils.isEmpty(spu.getImages())){
                    criteria.andEqualTo("images",spu.getImages());
            }
            // 售后服务
            if(!StringUtils.isEmpty(spu.getSaleService())){
                    criteria.andEqualTo("saleService",spu.getSaleService());
            }
            // 介绍
            if(!StringUtils.isEmpty(spu.getIntroduction())){
                    criteria.andEqualTo("introduction",spu.getIntroduction());
            }
            // 规格列表
            if(!StringUtils.isEmpty(spu.getSpecItems())){
                    criteria.andEqualTo("specItems",spu.getSpecItems());
            }
            // 参数列表
            if(!StringUtils.isEmpty(spu.getParaItems())){
                    criteria.andEqualTo("paraItems",spu.getParaItems());
            }
            // 销量
            if(!StringUtils.isEmpty(spu.getSaleNum())){
                    criteria.andEqualTo("saleNum",spu.getSaleNum());
            }
            // 评论数
            if(!StringUtils.isEmpty(spu.getCommentNum())){
                    criteria.andEqualTo("commentNum",spu.getCommentNum());
            }
            // 是否上架
            if(!StringUtils.isEmpty(spu.getIsMarketable())){
                    criteria.andEqualTo("isMarketable",spu.getIsMarketable());
            }
            // 是否启用规格
            if(!StringUtils.isEmpty(spu.getIsEnableSpec())){
                    criteria.andEqualTo("isEnableSpec",spu.getIsEnableSpec());
            }
            // 是否删除
            if(!StringUtils.isEmpty(spu.getIsDelete())){
                    criteria.andEqualTo("isDelete",spu.getIsDelete());
            }
            // 审核状态
            if(!StringUtils.isEmpty(spu.getStatus())){
                    criteria.andEqualTo("status",spu.getStatus());
            }
        }
        return example;
    }

    /**
     * 删除
     * @param id
     */
    @Override
    public void delete(Long id){
        Spu spu = spuMapper.selectByPrimaryKey(id);
        if ("0".equals(spu.getIsDelete())){
            throw new RuntimeException("无法删除未逻辑删除的商品");
        }
        spuMapper.deleteByPrimaryKey(id);
    }

    /**
     * 修改Spu
     * @param spu
     */
    @Override
    public void update(Spu spu){
        spuMapper.updateByPrimaryKey(spu);
    }

    /**
     * 增加Spu
     * @param spu
     */
    @Override
    public void add(Spu spu){
        spuMapper.insert(spu);
    }

    /**
     * 根据ID查询Spu
     * @param id
     * @return
     */
    @Override
    public Spu findById(Long id){
        return  spuMapper.selectByPrimaryKey(id);
    }

    /**
     * 查询Spu全部数据
     * @return
     */
    @Override
    public List<Spu> findAll() {
        return spuMapper.selectAll();
    }
}
