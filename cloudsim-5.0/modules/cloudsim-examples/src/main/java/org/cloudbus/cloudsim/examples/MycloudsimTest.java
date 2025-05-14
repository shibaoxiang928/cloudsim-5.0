package org.cloudbus.cloudsim.examples;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

public class MycloudsimTest {
    // 虚拟机列表
    private static List<Vm> vmlist;
    // 云任务列表
    private static List<Cloudlet> cloudletList;

    public static void main(String[] args) {
        Log.printLine("Starting Mycloudsim6...");
        try {
            // 云用户数量
            int num_user = 1;
            // 日历的字段已使用当前日期和时间初始化。
            Calendar calendar = Calendar.getInstance();
            // 跟踪事件
            boolean trace_flag = false;
            // 初始化CloudSim库。
            CloudSim.init(num_user, calendar, trace_flag);
            // 第二步：创建两个数据中心
            Datacenter datacenter0 = createDatacenter("Datacenter_0");
            Datacenter datacenter1 = createDatacenter("Datacenter_1");
            // 第三步：创建代理
            DatacenterBroker broker = createBroker();
            int brokerId = broker.getId();
            // 第四步：创建虚拟机
            vmlist = createVm(brokerId, 2);
            // 第五步：创建云任务
            cloudletList = createCloudlet(brokerId, 4);
            // 将虚拟机提交给代理
            broker.submitVmList(vmlist);
            // 将云任务列表提交给代理
            broker.submitCloudletList(cloudletList);
            // 第六步：开始模拟
            CloudSim.startSimulation();
            // 最后一步：模拟结束时打印结果
            List<Cloudlet> newList = broker.getCloudletReceivedList();
            printCloudletList(newList);
            Log.printLine("MyCloudSim finished!");
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
        }
    }

    // 创建云任务
    private static List<Cloudlet> createCloudlet(int brokerId, int cloudlets) {
        LinkedList<Cloudlet> list = new LinkedList<Cloudlet>();
        long length = 1000;
        long fileSize = 300;
        long outputSize = 300;
        int pesNumber = 1;
        UtilizationModel utilizationModel = new UtilizationModelFull();

        Cloudlet[] cloudlet = new Cloudlet[cloudlets];

        for (int i = 0; i < cloudlets; i++) {
            cloudlet[i] = new Cloudlet(i, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
            cloudlet[i].setUserId(brokerId);
            list.add(cloudlet[i]);
        }

        return list;
    }

    // 创建虚拟机
    private static List<Vm> createVm(int brokerId, int vms) {
        LinkedList<Vm> list = new LinkedList<Vm>();
        // Vm 描述
        int mips = 1000; // 速率
        long size = 10000; // 虚拟机的存储大小 (MB)
        int ram = 512; // 虚拟机内存 (MB)
        long bw = 1000; // 虚拟机带宽
        int pesNumber = 1; // cpu核数
        String vmm = "Xen"; // 虚拟机监视器

        Vm[] vm = new Vm[vms];

        for (int i = 0; i < vms; i++) {
            vm[i] = new Vm(i, brokerId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());
            list.add(vm[i]);
        }

        return list;
    }

    // 创建数据中心
    private static Datacenter createDatacenter(String name) {
        // 定义主机列表
        List<Host> hostList = new ArrayList<Host>();
        // 创建主机包含的PE或者CPU处理器列表，定义为MIPS速率
        List<Pe> peList1 = new ArrayList<Pe>();
        List<Pe> peList2 = new ArrayList<Pe>();
        int mips = 1000;

        // 创建处理器，并添加到Pe列表中
        peList1.add(new Pe(0, new PeProvisionerSimple(mips)));
        peList1.add(new Pe(1, new PeProvisionerSimple(mips)));
        peList1.add(new Pe(2, new PeProvisionerSimple(mips)));
        peList1.add(new Pe(3, new PeProvisionerSimple(mips)));
        peList2.add(new Pe(0, new PeProvisionerSimple(mips)));
        peList2.add(new Pe(1, new PeProvisionerSimple(mips)));

        // 创建主机，并将其添加至主机列表
        int hostId = 0;
        int ram = 2048; // 主机内存 (MB)
        long storage = 1000000; // 主机的存储空间
        int bw = 10000; // 主机带宽

        hostList.add(
                new Host(
                        hostId,
                        new RamProvisionerSimple(ram),
                        new BwProvisionerSimple(bw),
                        storage,
                        peList1,
                        new VmSchedulerTimeShared(peList1)
                )
        );

        hostId++;
        hostList.add(
                new Host(
                        hostId,
                        new RamProvisionerSimple(ram),
                        new BwProvisionerSimple(bw),
                        storage,
                        peList2,
                        new VmSchedulerTimeShared(peList2)
                )
        );

        String arch = "x86"; // 架构
        String os = "Linux"; // 操作系统
        String vmm = "Xen"; // 虚拟机监视器
        double time_zone = 10.0; // 此资源所在的时区
        double cost = 3.0; // 处理器花费
        double costPerMem = 0.05; // 内存花费
        double costPerStorage = 0.001; // 存储花费
        double costPerBw = 0.0; // 带宽花费
        LinkedList<Storage> storageList = new LinkedList<Storage>(); // 我们现在不添加SAN设备
        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, time_zone, cost, costPerMem,
                costPerStorage, costPerBw);

        // 创建一个PowerDatacenter对象。
        Datacenter datacenter = null;
        try {
            datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return datacenter;
    }

    // 创建代理
    private static DatacenterBroker createBroker() {
        DatacenterBroker broker = null;
        try {
            broker = new DatacenterBroker("Broker");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return broker;
    }

    // 打印云任务列表
    private static void printCloudletList(List<Cloudlet> list) {
        int size = list.size();
        Cloudlet cloudlet;

        String indent = "    ";
        Log.printLine();
        Log.printLine("========== OUTPUT ==========");
        Log.printLine("Cloudlet ID" + indent + "STATUS" + indent
                + "Data center ID" + indent + "VM ID" + indent + "Time" + indent
                + "Start Time" + indent + "Finish Time");

        DecimalFormat dft = new DecimalFormat("###.##");
        for (int i = 0; i < size; i++) {
            cloudlet = list.get(i);
            Log.print(indent + cloudlet.getCloudletId() + indent + indent);

            if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
                Log.print("SUCCESS");

                Log.printLine(indent + indent + cloudlet.getResourceId()
                        + indent + indent + indent + cloudlet.getVmId()
                        + indent + indent
                        + dft.format(cloudlet.getActualCPUTime()) + indent
                        + indent + dft.format(cloudlet.getExecStartTime())
                        + indent + indent
                        + dft.format(cloudlet.getFinishTime()));
            }
        }
    }
}