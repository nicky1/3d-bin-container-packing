package com.github.skjolber.packing.packer.plain;

import java.util.*;
import java.util.stream.Collectors;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerStackValue;
import com.github.skjolber.packing.api.DefaultContainer;
import com.github.skjolber.packing.api.DefaultStack;
import com.github.skjolber.packing.api.PackResultComparator;
import com.github.skjolber.packing.api.StackConstraint;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.StackValue;
import com.github.skjolber.packing.api.Stackable;
import com.github.skjolber.packing.api.ep.Point3D;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplier;
import com.github.skjolber.packing.ep.points3d.ExtremePoints3D;
import com.github.skjolber.packing.packer.AbstractPackagerBuilder;
import com.github.skjolber.packing.packer.DefaultPackResult;
import com.github.skjolber.packing.packer.DefaultPackResultComparator;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 * Selects the box with the highest volume first, then places it into the point with the lowest volume.
 * <br>
 * <br>
 * Thread-safe implementation. The input Boxes must however only be used in a single thread at a time.
 */

public class PlainPackager extends AbstractPlainPackager {

	public enum SortType {
		VOLUME_DESC,
		WEIGHT_ASC,
		ID_GROUPING,
		AREA_DESC;

		public Comparator<Stackable> comparator() {
			switch (this) {
				case VOLUME_DESC:
					return Comparator.comparingLong(Stackable::getVolume).reversed();
				case WEIGHT_ASC:
					return Comparator.comparingInt(Stackable::getWeight);
				case ID_GROUPING:
					return Comparator.comparing(Stackable::getId);
				case AREA_DESC:
					return Comparator.comparingLong(Stackable::getMinimumArea).reversed();
				default:
					throw new IllegalArgumentException();
			}
		}
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder extends AbstractPackagerBuilder<PlainPackager, Builder> {

		public PlainPackager build() {
			if(packResultComparator == null) {
				packResultComparator = new DefaultPackResultComparator();
			}
			return new PlainPackager(packResultComparator);
		}
	}

	public PlainPackager(PackResultComparator packResultComparator) {
		super(packResultComparator);
	}

	public DefaultPackResult pack(List<Stackable> stackables, Container targetContainer, int index, PackagerInterruptSupplier interrupt) {
		return pack(stackables, targetContainer, index, interrupt, EnumSet.allOf(SortType.class));
	}

	public DefaultPackResult pack(List<Stackable> stackables, Container targetContainer, int index, PackagerInterruptSupplier interrupt, Set<SortType> strategies) {
		DefaultPackResult bestResult = null;

		for (SortType strategy : strategies) {
			List<Stackable> sorted = new ArrayList<>(stackables);
			sorted.sort(strategy.comparator());
			
			DefaultPackResult result = tryPack(sorted, targetContainer, index, interrupt);
			if (result != null && (bestResult == null || result.getSize() > bestResult.getSize())) {
				bestResult = result;
			}
		}
		return bestResult;
	}

	private DefaultPackResult tryPack(List<Stackable> stackables, Container targetContainer, int index, PackagerInterruptSupplier interrupt) {
		List<Stackable> remainingStackables = new ArrayList<>(stackables);

		ContainerStackValue[] stackValues = targetContainer.getStackValues();

		ContainerStackValue containerStackValue = stackValues[0];

		StackConstraint constraint = containerStackValue.getConstraint();

		DefaultStack stack = new DefaultStack(containerStackValue);

		List<Stackable> scopedStackables = stackables
				.stream()
				.filter(s -> s.getVolume() <= containerStackValue.getMaxLoadVolume() && s.getWeight() <= targetContainer.getMaxLoadWeight())
				.filter(s -> constraint == null || constraint.canAccept(s))
				.collect(Collectors.toList());

		ExtremePoints3D extremePoints3D = new ExtremePoints3D(containerStackValue.getLoadDx(), containerStackValue.getLoadDy(), containerStackValue.getLoadDz());
		extremePoints3D.setMinimumAreaAndVolumeLimit(getMinStackableArea(scopedStackables), getMinStackableVolume(scopedStackables));

		int maxRemainingWeight = containerStackValue.getMaxLoadWeight();

		while (!extremePoints3D.isEmpty() && maxRemainingWeight > 0 && !scopedStackables.isEmpty()) {
			if(interrupt.getAsBoolean()) {
				// fit2d below might have returned due to deadline

				return null;
			}

			long maxPointVolume = extremePoints3D.getMaxVolume();
			long maxPointArea = extremePoints3D.getMaxArea();

			int bestPointIndex = -1;
			int bestIndex = -1;
			
			long bestPointSupportPercent = -1L;

			StackValue bestStackValue = null;
			Stackable bestStackable = null;
			String lastPlacedStackableId = stack.isEmpty() ? null : stack.getPlacements().get(stack.getPlacements().size() - 1).getStackable().getId();

			int currentPointsCount = extremePoints3D.getValueCount();
			for (int i = 0; i < scopedStackables.size(); i++) {
				Stackable box = scopedStackables.get(i);
				if(box.getVolume() > maxPointVolume) {
					continue;
				}
				if(box.getWeight() > maxRemainingWeight) {
					continue;
				}

				if(bestStackable != null && !isBetter(bestStackable, box)) {
					continue;
				}

				if(constraint != null && !constraint.accepts(stack, box)) {
					continue;
				}

				for (StackValue stackValue : box.getStackValues()) {
					if(stackValue.getArea() > maxPointArea) {
						continue;
					}
					if(stackValue.getVolume() > maxPointVolume) {
						continue;
					}

					for (int k = 0; k < currentPointsCount; k++) {
						Point3D point3d = extremePoints3D.getValue(k);

						if(!point3d.fits3D(stackValue)) {
							continue;
						}

						// Check constraints first
						if(constraint != null && !constraint.supports(stack, box, stackValue, point3d.getMinX(), point3d.getMinY(), point3d.getMinZ())) {
							continue;
						}

						long calculatedPointSupportPercent = calculateXYSupportPercent(extremePoints3D, point3d, stackValue); // Calculate support
						boolean updateBest = false;

						if (bestIndex == -1) { // No best placement found yet
							updateBest = true;
						} else {
							Point3D currentBestPoint = extremePoints3D.getValue(bestPointIndex);

							// Criterion 1: Lower Z is better
							if (point3d.getMinZ() < currentBestPoint.getMinZ()) {
								updateBest = true;
							} else if (point3d.getMinZ() == currentBestPoint.getMinZ()) {
								// Criterion 2: Lower X is better (for same Z)
								if (point3d.getMinX() < currentBestPoint.getMinX()) {
									updateBest = true;
								} else if (point3d.getMinX() == currentBestPoint.getMinX()) {
									// Criterion 3: Prefer same item ID (for same Z and X)
									boolean currentIsSameId = lastPlacedStackableId != null && box.getId().equals(lastPlacedStackableId);
									boolean bestIsSameId = bestStackable != null && lastPlacedStackableId != null && bestStackable.getId().equals(lastPlacedStackableId);

									if (currentIsSameId && !bestIsSameId) {
										updateBest = true;
									} else if (currentIsSameId == bestIsSameId) {
										// Criterion 4: Higher support is better (for same Z, X, and same ID preference)
										if (calculatedPointSupportPercent > bestPointSupportPercent) {
											updateBest = true;
										} else if (calculatedPointSupportPercent == bestPointSupportPercent) {
											// Criterion 5: Larger item area is better (for same Z, X, same ID preference, and support)
											if (bestStackValue == null || stackValue.getArea() > bestStackValue.getArea()) {
												updateBest = true;
											}
										}
									}
								}
							}
						}

						if (updateBest) {
							bestPointSupportPercent = calculatedPointSupportPercent;
							bestPointIndex = k;
							bestIndex = i;
							bestStackValue = stackValue;
							bestStackable = box;
						}
					}
				}
			}

			if(bestIndex == -1) {
				break;
			}
			
			scopedStackables.remove(bestIndex);
			remainingStackables.remove(bestStackable);

			Point3D point = extremePoints3D.getValue(bestPointIndex);

			StackPlacement stackPlacement = new StackPlacement(bestStackable, bestStackValue, point.getMinX(), point.getMinY(), point.getMinZ());
			stack.add(stackPlacement);
			extremePoints3D.add(bestPointIndex, stackPlacement);

			if(!scopedStackables.isEmpty()) {
				boolean minArea = bestStackValue.getArea() == extremePoints3D.getMinAreaLimit();
				boolean minVolume = extremePoints3D.getMinVolumeLimit() == bestStackable.getVolume();
				if(minArea && minVolume) {
					extremePoints3D.setMinimumAreaAndVolumeLimit(getMinStackableArea(scopedStackables), getMinStackableVolume(scopedStackables));
				} else if(minArea) {
					extremePoints3D.setMinimumAreaLimit(getMinStackableArea(scopedStackables));
				} else if(minVolume) {
					extremePoints3D.setMinimumVolumeLimit(getMinStackableVolume(scopedStackables));
				}
			}

			maxRemainingWeight -= bestStackable.getWeight();
		}

		return new DefaultPackResult(new DefaultContainer(targetContainer.getId(), targetContainer.getDescription(), targetContainer.getVolume(), targetContainer.getEmptyWeight(), stackValues, stack),
				stack, true, index);
	}

	protected long calculateXYSupportPercent(ExtremePoints3D extremePoints3D, Point3D referencePoint, StackValue stackValue) {
		long sum = 0;

		int minX = referencePoint.getMinX();
		int minY = referencePoint.getMinY();
		
		int maxX = minX + stackValue.getDx() - 1; // inclusive
		int maxY = minY + stackValue.getDy() - 1; // inclusive
		
		long max = (maxX - minX + 1) * (maxY - minY + 1);
		
		int z = referencePoint.getMinZ() - 1;
		
		List<StackPlacement> placements = extremePoints3D.getPlacements();
		for(StackPlacement stackPlacement : placements) {
			if(stackPlacement.getAbsoluteEndZ() == z) {
				
				// calculate the common area
				// check too far
				if(stackPlacement.getAbsoluteX() > maxX) {
					continue;
				}
				
				if(stackPlacement.getAbsoluteY() > maxY) {
					continue;
				}
				
				if(stackPlacement.getAbsoluteEndX() < minX) {
					continue;
				}
				
				if(stackPlacement.getAbsoluteEndY() < minY) {
					continue;
				}
				
				// placement can support the stack value
				
				// |           
				// |           |---------|
				// |           |         | 
				// |    |-----------|    |
				// |    |      |xxxx|    |
				// |    |      -----|----|
				// |    |           |
				// |    |-----------| 
				// |
				// --------------------------------
				
			    int x1 = Math.max(stackPlacement.getAbsoluteX(), minX);
			    int y1 = Math.max(stackPlacement.getAbsoluteY(), minY);
			 
			    // gives top-right point
			    // of intersection rectangle
			    int x2 = Math.min(stackPlacement.getAbsoluteEndX(), maxX);
			    int y2 = Math.min(stackPlacement.getAbsoluteEndY(), maxY);
				
			    long intersect = (x2 - x1 + 1) * (y2 - y1 + 1);
			    
			    sum += intersect;
			    
			    if(sum == max) {
			    	break;
			    }
			}
		}
		
		return (sum * 100) / stackValue.getArea();
	}

	protected boolean isBetter(Stackable referenceStackable, Stackable potentiallyBetterStackable) {
		// ****************************************
		// * Prefer the highest volume
		// ****************************************

		if(referenceStackable.getVolume() == potentiallyBetterStackable.getVolume()) {
			return referenceStackable.getWeight() < potentiallyBetterStackable.getWeight();
		}
		return referenceStackable.getVolume() < potentiallyBetterStackable.getVolume();
	}

	@Override
	public PlainPackagerResultBuilder newResultBuilder() {
		return new PlainPackagerResultBuilder().withPackager(this);
	}
}
