package com.github.skjolber.packing.visualizer.packaging.dto;

import com.alibaba.excel.annotation.ExcelProperty;

/**
 * @desc:
 * @author: yixiaoshuang
 * @date: 2025/6/25
 **/
public class CabSkuPositionExcelDto {
	@ExcelProperty("产品编码")
	private String productCode;
	@ExcelProperty("X坐标")
	private Double x;
	@ExcelProperty("Y坐标")
	private Double y;
	@ExcelProperty("Z坐标")
	private Double z;
	@ExcelProperty("原始尺寸(L×W×H)")
	private String stackable;
	@ExcelProperty("摆放尺寸(L×W×H)")
	private String orientation;

	public String getProductCode() {
		return productCode;
	}

	public void setProductCode(String productCode) {
		this.productCode = productCode;
	}

	public Double getX() {
		return x;
	}

	public void setX(Double x) {
		this.x = x;
	}

	public Double getY() {
		return y;
	}

	public void setY(Double y) {
		this.y = y;
	}

	public Double getZ() {
		return z;
	}

	public void setZ(Double z) {
		this.z = z;
	}

	public String getStackable() {
		return stackable;
	}

	public void setStackable(String stackable) {
		this.stackable = stackable;
	}

	public String getOrientation() {
		return orientation;
	}

	public void setOrientation(String orientation) {
		this.orientation = orientation;
	}

	@Override
	public String toString() {
		return "CabSkuPositionExcelDto{" +
			"productCode='" + productCode + '\'' +
			", x=" + x +
			", y=" + y +
			", z=" + z +
			", stackable='" + stackable + '\'' +
			", orientation='" + orientation + '\'' +
			'}';
	}
}
