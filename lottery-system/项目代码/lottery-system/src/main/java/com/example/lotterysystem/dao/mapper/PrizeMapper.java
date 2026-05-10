package com.example.lotterysystem.dao.mapper;

import com.example.lotterysystem.dao.dataobject.PrizeDO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PrizeMapper {

    @Insert("insert into prize (name, image_url, price, description, probability, stock)" +
            " values (#{name}, #{imageUrl}, #{price}, #{description}, #{probability}, #{stock})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(PrizeDO prizeDO);

    @Select("select count(1) from prize")
    int count();

    @Select("select * from prize order by id desc limit #{offset}, #{pageSize}")
    List<PrizeDO> selectPrizeList(@Param("offset") Integer offset,
                                  @Param("pageSize") Integer pageSize);

    @Select("<script>" +
            " select id from prize" +
            " where id in" +
            " <foreach item='item' collection='items' open='(' separator=',' close=')'>" +
            " #{item}" +
            " </foreach>" +
            " </script>")
    List<Long> selectExistByIds(@Param("items") List<Long> ids);

    @Select("<script>" +
            " select * from prize" +
            " where id in" +
            " <foreach item='item' collection='items' open='(' separator=',' close=')'>" +
            " #{item}" +
            " </foreach>" +
            " </script>")
    List<PrizeDO> batchSelectByIds(@Param("items") List<Long> ids);

    @Select("select * from prize where id = #{id}")
    PrizeDO selectById(@Param("id") Long id);

    /**
     * 更新奖品概率
     */
    @Update("UPDATE prize SET probability = #{probability} WHERE id = #{id}")
    int updateProbability(@Param("id") Long id, @Param("probability") Double probability);

    /**
     * 更新奖品库存
     */
    @Update("UPDATE prize SET stock = #{stock} WHERE id = #{id}")
    int updateStock(@Param("id") Long id, @Param("stock") Integer stock);

    /**
     * 扣减库存（原子操作）
     */
    @Update("UPDATE prize SET stock = stock - 1 WHERE id = #{id} AND stock > 0")
    int decrementStock(@Param("id") Long id);

    /**
     * 批量更新奖品概率和库存
     */
    @Update("<script>" +
            "<foreach collection='list' item='item' separator=';'>" +
            "UPDATE prize SET probability = #{item.probability}, stock = #{item.stock} WHERE id = #{item.id}" +
            "</foreach>" +
            "</script>")
    int batchUpdate(@Param("list") List<PrizeDO> prizes);
}