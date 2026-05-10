package com.example.lotterysystem.mapper;

import com.example.lotterysystem.entity.Prize;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PrizeMapper {

    @Select("SELECT * FROM prize ORDER BY level ASC, id DESC")
    List<Prize> findAll();

    @Select("SELECT * FROM prize WHERE id = #{id}")
    Prize findById(Integer id);

    @Select("SELECT * FROM prize WHERE name = #{name}")
    List<Prize> findByName(String name);

    @Select("SELECT * FROM prize WHERE level = #{level}")
    List<Prize> findByLevel(Integer level);

    @Insert("INSERT INTO prize(name, description, stock, remaining, level, image_url) VALUES(#{name}, #{description}, #{stock}, #{remaining}, #{level}, #{imageUrl})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Prize prize);

    @Update("UPDATE prize SET name = #{name}, description = #{description}, stock = #{stock}, remaining = #{remaining}, level = #{level}, image_url = #{imageUrl} WHERE id = #{id}")
    int update(Prize prize);

    @Update("UPDATE prize SET remaining = remaining - 1 WHERE id = #{id} AND remaining > 0")
    int decreaseRemaining(Integer id);

    @Update("UPDATE prize SET stock = stock + #{count}, remaining = remaining + #{count} WHERE id = #{id}")
    int increaseStock(@Param("id") Integer id, @Param("count") Integer count);

    @Update("UPDATE prize SET image_url = #{imageUrl} WHERE id = #{id}")
    int updateImage(@Param("id") Integer id, @Param("imageUrl") String imageUrl);

    @Delete("DELETE FROM prize WHERE id = #{id}")
    int deleteById(Integer id);

    @Select("SELECT COUNT(*) FROM prize")
    int count();

    // ==================== 库存预警方法 ====================

    @Select("SELECT * FROM prize WHERE remaining <= #{threshold} AND remaining > 0")
    List<Prize> findLowStockPrizes(@Param("threshold") Integer threshold);

    @Select("SELECT * FROM prize WHERE remaining = 0 AND stock > 0")
    List<Prize> findSoldOutPrizes();

    @Select("SELECT COUNT(*) FROM prize WHERE remaining <= #{threshold} AND remaining > 0")
    int getLowStockCount(@Param("threshold") Integer threshold);
}