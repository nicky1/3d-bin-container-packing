package com.github.skjolber.packing.visualizer.packaging;

import com.github.skjolber.packing.api.*;
import com.github.skjolber.packing.packer.laff.LargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.plain.PlainPackager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @desc:
 * @author: yixiaoshuang
 * @date: 2025/6/6
 **/
public class TestLoadResult {
    public static void main(String[] args) {
        List<StackableItem> products = new ArrayList<StackableItem>();

		products.add(new StackableItem(Box.newBuilder().withId("USAN1040288-3").withSize(315, 1440, 250).withRotate3D().withWeight(296 ).build(), 233 ));
		products.add(new StackableItem(Box.newBuilder().withId("USAN1040288-20").withSize(315,1580 , 250).withRotate3D().withWeight(320).build(), 362 ));

//		products.add(new StackableItem(Box.newBuilder().withId("USAN1040288-3").withSize(47, 205, 26).withRotate3D().withWeight(64).build(), 157));
//		products.add(new StackableItem(Box.newBuilder().withId("USAN1040288-20").withSize(48,164 , 24).withRotate3D().withWeight(60).build(), 177));

//		products.add(new StackableItem(Box.newBuilder().withId("1040953-2").withSize(62, 163, 12).withRotate3D().withWeight(35).build(), 222));
//		products.add(new StackableItem(Box.newBuilder().withId("1041261-10").withSize(56,148 , 13).withRotate3D().withWeight(31).build(), 383));

//        products.add(new StackableItem(Box.newBuilder().withId("USAN1070376-2").withSize(580, 1670, 180).withRotate3D().withWeight(495).build(), 140));
//        products.add(new StackableItem(Box.newBuilder().withId("USAN1040288-3").withSize(470, 2045, 255).withRotate3D().withWeight(640).build(), 6));
//        products.add(new StackableItem(Box.newBuilder().withId("USAN1040288-11").withSize(480, 1640, 240).withRotate3D().withWeight(596).build(), 21));
//        products.add(new StackableItem(Box.newBuilder().withId("USAN1040288-7").withSize(380, 1485, 320).withRotate3D().withWeight(558).build(), 40));
//        products.add(new StackableItem(Box.newBuilder().withId("USAN1040288AA-4").withSize(380, 1485, 320).withRotate3D().withWeight(558).build(), 18));
//        products.add(new StackableItem(Box.newBuilder().withId("USAN1040288AA-5").withSize(480, 1640, 240).withRotate3D().withWeight(596).build(), 100));
//        products.add(new StackableItem(Box.newBuilder().withId("USAN1036840-5").withSize(600, 1645, 175).withRotate3D().withWeight(510).build(), 15));
//        products.add(new StackableItem(Box.newBuilder().withId("USAN1040288-19").withSize(380, 1485, 320).withRotate3D().withWeight(558).build(), 35));
//        products.add(new StackableItem(Box.newBuilder().withId("USAN1040288-20").withSize(480, 1640, 240).withRotate3D().withWeight(596).build(), 20));

        int total = products.stream().mapToInt(StackableItem::getCount).sum();
        // add a single container type
        Container container = Container.newBuilder()
                .withDescription("40HQ")
                .withSize(2360, 12040, 2700)
                .withEmptyWeight(0)
                .withMaxLoadWeight(240000)
                .build();

        List<ContainerItem> containerItems = ContainerItem
                .newListBuilder()
                .withContainer(container, 1)
                .build();

//		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager
//			.newBuilder()
//			.build();

        PlainPackager packager = PlainPackager
                .newBuilder()
                .build();

        PackagerResult result = packager
                .newResultBuilder()
                .withMaxContainerCount(1)
                .withContainers(containerItems)
                .withStackables(products)
                .build();

        if (result.isSuccess()) {
            Container c = result.get(0);
            List<Container> cabs = new ArrayList<>();
            cabs.add(c);
            List<StackPlacement> placements = c.getStack().getPlacements();
            System.out.println("装载货物数量：" + placements.size() + ";总量：" + total);
            DefaultPackagingResultVisualizerFactory p = new DefaultPackagingResultVisualizerFactory(false);

            File file = new File("/Users/yixiaoshuang/logs/cabinet/containers.json");
            try {
                p.visualize(cabs, file);
            } catch (Exception e) {

            }
        }
    }
}
