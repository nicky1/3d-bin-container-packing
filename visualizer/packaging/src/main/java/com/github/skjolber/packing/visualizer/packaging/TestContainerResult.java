package com.github.skjolber.packing.visualizer.packaging;

import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.PageReadListener;
import com.alibaba.fastjson2.JSON;
import com.github.skjolber.packing.visualizer.packaging.dto.CabSkuPositionExcelDto;
import com.github.skjolber.packing.visualizer.packaging.dto.SkuPositionDto;
import org.apache.poi.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @desc:
 * @author: yixiaoshuang
 * @date: 2025/6/25
 **/
public class TestContainerResult {

	public static void main(String[] args) {
		String path = "/Users/yixiaoshuang/aukeys/业务介绍/ERP/采购模块/装箱/装箱样例数据2-集装箱14装载明细.xlsx";
		List<CabSkuPositionExcelDto> dataList = new ArrayList<>();
		EasyExcel.read(path, CabSkuPositionExcelDto.class, new PageReadListener<CabSkuPositionExcelDto>(d -> {
			dataList.addAll(d);
		})).sheet("集装箱14_已装载").doRead();

		System.out.println(dataList);
		List<SkuPositionDto> list = new ArrayList<>();
		int  i = 1;
		for (CabSkuPositionExcelDto dto : dataList) {
			i++;
			SkuPositionDto skuPositionDto = new SkuPositionDto();
			skuPositionDto.setInstanceId(dto.getProductCode()+"-"+i);
			skuPositionDto.setProductCode(dto.getProductCode());
			skuPositionDto.setProductId(skuPositionDto.getInstanceId());
			skuPositionDto.setPosition(new Double[]{dto.getX(),dto.getY(),dto.getZ()});
			String stackable = dto.getStackable();
			List<String> stList = StrUtil.split(stackable, '×');
			skuPositionDto.setStackable(new Double[]{Double.valueOf(stList.get(0)),Double.valueOf(stList.get(1)),Double.valueOf(stList.get(2))});
			String placement = dto.getOrientation();
			List<String> plList = StrUtil.split(placement, '×');
			skuPositionDto.setOrientation(new Double[]{Double.valueOf(plList.get(0)),Double.valueOf(plList.get(1)),Double.valueOf(plList.get(2))});
			list.add(skuPositionDto);
		}

		System.out.println(JSON.toJSONString(list));

	}
}
