import * as THREE from "three";
import { Color, Mesh, Object3D, Scene } from "three";
import randomColor from "randomcolor";

export class Point {

    x: number;
    y: number;
    z: number;

    dx: number;
    dy: number;
    dz: number;

    constructor(x: number, y: number, z: number, dx: number, dy: number, dz: number) {
        this.x = x;
        this.y = y;
        this.z = z;

        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
    }
}

export class Stackable {

    dx: number;
    dy: number;
    dz: number;

    name: string;
    id: string;

    step: number;

    constructor(name: string, id: string, step: number, dx: number, dy: number, dz: number) {
        this.name = name;
        this.id = id;
        this.step = step;
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
    }

}

export class Box extends Stackable {

    constructor(name: string, id: string, step: number, dx: number, dy: number, dz: number) {
        super(name, id, step, dx, dy, dz);
    }

}

export class Container extends Stackable {

    loadDx: number;
    loadDy: number;
    loadDz: number;

    stack: Stack;

    constructor(name: string, id: string, step: number, dx: number, dy: number, dz: number, loadDx: number, loadDy: number, loadDz: number) {
        super(name, id, step, dx, dy, dz);

        this.loadDx = loadDx;
        this.loadDy = loadDy;
        this.loadDz = loadDz;

        this.stack = new Stack(step);
    }

    add(stackPlacement: StackPlacement) {
        this.stack.add(stackPlacement);
    }
}

export class StackPlacement {

    stackable: Stackable;
    x: number;
    y: number;
    z: number;

    step: number;

    points: Array<Point>;

    constructor(stackable: Stackable, step: number, x: number, y: number, z: number, points: Array<Point>) {
        this.stackable = stackable;
        this.step = step;
        this.x = x;
        this.y = y;
        this.z = z;
        this.points = points;
    }

}

export class Stack {

    placements: Array<StackPlacement>;

    step: number;

    constructor(step: number) {
        this.step = step;
        this.placements = new Array();
    }

    add(placement: StackPlacement) {
        this.placements.push(placement);
    }

}

export class ContainerControls {

    parent: Object3D;
    child: Object3D;
    container: Container;

    constructor(parent: Object3D, child: Object3D, container: Container) {
        this.parent = parent;
        this.child = child;
        this.container = container;
    }
}

export interface ColorScheme {
    getPoint(point: Point): Color;
    getStackable(stackable: Stackable): Color;
    getColorScheme(container: Container): ColorScheme;
}

export class RandomColorScheme implements ColorScheme {
    getPoint(point: Point): Color {
        return new Color(randomColor());
    }
    getStackable(stackable: Stackable): Color {
        return new Color(randomColor());
    }
    getColorScheme(container: Container): ColorScheme {
        return this;
    }

}

export class MemoryColorScheme implements ColorScheme {

    delegate: ColorScheme;
    map: Map<string, Color>;

    constructor(delegate: ColorScheme) {
        this.delegate = delegate;
        this.map = new Map();
    }

    getStackable(stackable: Stackable): Color {
        if (!stackable.id) {
            // use random
            return this.delegate.getStackable(stackable);
        }
        // use same as before, for the
        var color = this.map.get(stackable.id);
        if (!color) {
            color = this.delegate.getStackable(stackable);
            this.map.set(stackable.id, color);
        }
        return color;
    }

    getColorScheme(container: Container): ColorScheme {
        return this;
    }

    getPoint(point: Point): Color {
        // use same as before, for the
        var id = point.x + "x" + point.y + "x" + point.z + " " + point.dx + "x" + point.dy + "x" + point.dz;
        var color = this.map.get(id);
        if (!color) {
            color = this.delegate.getPoint(point);
            this.map.set(id, color);
        }
        return color;
    }

}

export class StackableRenderer {

    add(parent: Object3D, colorScheme: ColorScheme, stackPlacement: StackPlacement, x: number, y: number, z: number): Object3D | undefined {
        var stackable = stackPlacement.stackable;
        if (stackable instanceof Container) {
            var containerStackable: Container = stackable;
            var color = colorScheme.getStackable(containerStackable);
            var containerMaterial = new THREE.LineBasicMaterial({ color: '#0099FF' });

            var containerGeometry = new THREE.EdgesGeometry(new THREE.BoxGeometry(containerStackable.dy, containerStackable.dz, containerStackable.dx));
            var containerLoadGeometry = new THREE.EdgesGeometry(new THREE.BoxGeometry(containerStackable.loadDy, containerStackable.loadDz, containerStackable.loadDx));

            var container = new THREE.LineSegments(containerGeometry, containerMaterial);
            var containerLoad = new THREE.LineSegments(containerLoadGeometry, containerMaterial);

            container.position.x = stackPlacement.y + containerStackable.dy / 2 + x;
            container.position.y = stackPlacement.z + containerStackable.dz / 2 + y;
            container.position.z = stackPlacement.x + containerStackable.dx / 2 + z;

            var offsetX = - containerStackable.dy / 2;
            var offsetY = - containerStackable.dz / 2;
            var offsetZ = - containerStackable.dx / 2;

            console.log("Add container " + containerStackable.name + " size " + containerStackable.dx + "x" + containerStackable.dy + "x" + containerStackable.dz + " with load " + containerStackable.loadDx + "x" + containerStackable.loadDy + "x" + containerStackable.loadDz + " at " + stackPlacement.x + "," + stackPlacement.y + "," + stackPlacement.z);

            container.name = containerStackable.name;
            container.userData = containerStackable;

            container.userData = {
                step: 0,
                type: "container",
                source: container
            };

            containerLoad.name = containerStackable.name;
            containerLoad.userData = {
                step: 0,
                type: "containerLoad",
                offsetX: offsetX,
                offsetY: offsetY,
                offsetZ: offsetZ
            };

            parent.add(container);
            container.add(containerLoad);

            var nextColorScheme = colorScheme.getColorScheme(containerStackable);
            var i = 0;
            containerStackable.stack.placements.forEach((placement) => {
                if (i < 1433) {
                    this.add(containerLoad, nextColorScheme, placement, offsetX, offsetY, offsetZ);
                }
                i++;
            });

            return container;
        } else if (stackable instanceof Box) {
            var boxStackable: Box = stackable;
            // console.log("Add box " + boxStackable.id + " size " + boxStackable.dx + "x" + boxStackable.dy + "x" + boxStackable.dz + " at " + stackPlacement.x + "," + stackPlacement.y + "," + stackPlacement.z);
            var sku = boxStackable.id;
            if (sku == 'USAN1040953-2') {
                // return undefined;
            }
            // var sColor = boxStackable.id == 'USAN1040953-2' ? '#FFFF33' : '#FF9900';
            var sColor = colorScheme.getStackable(boxStackable);


            var material = new THREE.MeshStandardMaterial({
                color: sColor,
                opacity: 0.8,
                metalness: 0.2,
                roughness: 1,
                transparent: true,
                polygonOffset: true,
                polygonOffsetFactor: 1,
                polygonOffsetUnits: 1
            });
            material.color.convertSRGBToLinear();
            // Define the gap size (e.g., 0.1 units for each side, so total reduction is 0.2)
            // You can adjust this value as needed.
            // Consider making this configurable if different gap sizes are needed.
            const gap = 1; // This will be subtracted from each dimension
            const geometryDx = Math.max(0, boxStackable.dx - gap);
            const geometryDy = Math.max(0, boxStackable.dy - gap);
            const geometryDz = Math.max(0, boxStackable.dz - gap);

            var geometry = new THREE.BoxGeometry(geometryDy, geometryDz, geometryDx);
            var box = new THREE.Mesh(geometry, material);

            box.name = boxStackable.name;

            box.position.x = stackPlacement.y + boxStackable.dy / 2 + x;
            box.position.y = stackPlacement.z + boxStackable.dz / 2 + y;
            box.position.z = stackPlacement.x + boxStackable.dx / 2 + z;
            console.log("box position " + boxStackable.id + " size " + boxStackable.dx + "x" + boxStackable.dy + "x" + boxStackable.dz + " at " + box.position.x + "," + box.position.y + "," + box.position.z);

            box.userData = {
                step: boxStackable.step,
                type: "box",
                source: stackPlacement
            };
            // 创建边框
            const edgeGeometry = new THREE.EdgesGeometry(geometry); // 使用相同的缩小后的几何体
            const edgeMaterial = new THREE.LineBasicMaterial({ color: 0xffffff, linewidth: 10 }); // 白色边框，线宽为2
            const boxEdges = new THREE.LineSegments(edgeGeometry, edgeMaterial);
            // 将边框添加到箱子网格对象上，这样它们会一起移动和定位
            box.add(boxEdges);

            parent.add(box);

            return box;
        }

        return undefined;
    }

    makeBoxPosition(stackPlacement: StackPlacement,colorScheme: ColorScheme, x: number, y: number, z: number) {
        var stackable = stackPlacement.stackable;
        var boxStackable: Box = stackable;
        // console.log("Add box " + boxStackable.id + " size " + boxStackable.dx + "x" + boxStackable.dy + "x" + boxStackable.dz + " at " + stackPlacement.x + "," + stackPlacement.y + "," + stackPlacement.z);
        var sku = boxStackable.id;
        if (sku == 'USAN1040953-2') {
            // return undefined;
        }
        
        // var sColor = boxStackable.id == 'USAN1040953-2' ? 'red' : 'blue';
        var sColor = colorScheme.getStackable(boxStackable);
        var material = new THREE.MeshStandardMaterial({
            color: sColor,
            opacity: 0.8,
            metalness: 0.2,
            roughness: 1,
            transparent: true,
            polygonOffset: true,
            polygonOffsetFactor: 1,
            polygonOffsetUnits: 1
        });
        material.color.convertSRGBToLinear();
        // Define the gap size (e.g., 0.1 units for each side, so total reduction is 0.2)
        // You can adjust this value as needed.
        // Consider making this configurable if different gap sizes are needed.
        const gap = 0.5; // This will be subtracted from each dimension
        const geometryDx = Math.max(0, boxStackable.dx - gap);
        const geometryDy = Math.max(0, boxStackable.dy - gap);
        const geometryDz = Math.max(0, boxStackable.dz - gap);

        var geometry = new THREE.BoxGeometry(geometryDy, geometryDz, geometryDx);
        var box = new THREE.Mesh(geometry, material);

        box.name = boxStackable.id;

        box.position.x = stackPlacement.y + boxStackable.dy / 2 + x;
        box.position.y = stackPlacement.z + boxStackable.dz / 2 + y;
        box.position.z = stackPlacement.x + boxStackable.dx / 2 + z;
        // console.log("box position " + boxStackable.id + " size " + boxStackable.dx + "x" + boxStackable.dy + "x" + boxStackable.dz + " at " + box.position.x + "," + box.position.y + "," + box.position.z);

        box.userData = {
            step: boxStackable.step,
            type: "box",
            source: stackPlacement
        };
        // 创建边框
        const edgeGeometry = new THREE.EdgesGeometry(geometry); // 使用相同的缩小后的几何体
        const edgeMaterial = new THREE.LineBasicMaterial({ color: 0xffffff, linewidth: 5 }); // 白色边框，线宽为2
        const boxEdges = new THREE.LineSegments(edgeGeometry, edgeMaterial);
        // 将边框添加到箱子网格对象上，这样它们会一起移动和定位
        box.add(boxEdges);


        return box;
    }

    removePoints(container: Object3D) {
        var children = container.children;
        for (var j = 0; j < children.length; j++) {
            var userData = children[j].userData;

            if (userData.type == "containerLoad") {
                var containerLoad = children[j];
                var containerLoadChildren = containerLoad.children;

                for (var i = containerLoadChildren.length - 1; i >= 0; i--) {
                    var child = containerLoadChildren[i];
                    var userData = child.userData;

                    if (userData.type == "point") {
                        containerLoad.remove(child);
                    }
                }
            }
        }
    }

    addPoints(container: Object3D, colorScheme: ColorScheme, stepNumber: number, pointNumber: number) {

        var children = container.children;
        for (var j = 0; j < children.length; j++) {
            var userData = children[j].userData;

            if (userData.type == "containerLoad") {
                var containerLoad = children[j];
                var containerLoadChildren = containerLoad.children;

                for (var i = 0; i < containerLoadChildren.length; i++) {
                    var child = containerLoadChildren[i];

                    var containerLoadChildUserData = child.userData;

                    if (containerLoadChildUserData.type == "box" && containerLoadChildUserData.step == stepNumber - 1) {


                        for (var k = 0; k < containerLoadChildUserData.source.points.length; k++) {
                            var p = containerLoadChildUserData.source.points[k]
                            if (pointNumber == -1 || pointNumber == k) {

                                var color = colorScheme.getPoint(p);

                                var pointMaterial = new THREE.LineBasicMaterial({ color: color });
                                pointMaterial.color.convertSRGBToLinear();
                                var containerGeometry = new THREE.EdgesGeometry(new THREE.BoxGeometry(p.dy, p.dz, p.dx));
                                var pp = new THREE.LineSegments(containerGeometry, pointMaterial);

                                pp.position.x = p.y + p.dy / 2 + userData.offsetX;
                                pp.position.y = p.z + p.dz / 2 + userData.offsetY;
                                pp.position.z = p.x + p.dx / 2 + userData.offsetZ;

                                pp.userData = {
                                    type: "point"
                                };

                                pp.visible = true;

                                containerLoad.add(pp)
                            }
                        }
                        break;
                    }
                }
            }
        }

    }
}

