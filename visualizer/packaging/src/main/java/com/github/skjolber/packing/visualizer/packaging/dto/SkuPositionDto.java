package com.github.skjolber.packing.visualizer.packaging.dto;

/**
 * @desc:
 * @author: yixiaoshuang
 * @date: 2025/6/25
 **/
public class SkuPositionDto {
	private String instanceId;
	private String productId;
	private String productCode;
	private String stacking;
	private Double[] position;
	private Double[] rotation;
	// 摆放尺寸
	private Double[] orientation;
	// 货物尺寸
	private Double[] stackable;

	public Double[] getOrientation() {
		return orientation;
	}

	public void setOrientation(Double[] orientation) {
		this.orientation = orientation;
	}

	public Double[] getStackable() {
		return stackable;
	}

	public void setStackable(Double[] stackable) {
		this.stackable = stackable;
	}

	public String getInstanceId() {
		return instanceId;
	}

	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}

	public String getProductId() {
		return productId;
	}

	public void setProductId(String productId) {
		this.productId = productId;
	}

	public String getProductCode() {
		return productCode;
	}

	public void setProductCode(String productCode) {
		this.productCode = productCode;
	}

	public String getStacking() {
		return stacking;
	}

	public void setStacking(String stacking) {
		this.stacking = stacking;
	}

	public Double[] getPosition() {
		return position;
	}

	public void setPosition(Double[] position) {
		this.position = position;
	}

	public Double[] getRotation() {
		return rotation;
	}

	public void setRotation(Double[] rotation) {
		this.rotation = rotation;
	}
}
