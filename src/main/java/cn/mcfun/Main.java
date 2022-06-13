package cn.mcfun;

import cn.mcfun.request.GetRequest;
import cn.mcfun.utils.Hikari;
import cn.mcfun.utils.MyPrintStream;
import cn.mcfun.utils.MyRegExp;
import cn.mcfun.utils.Order;
import cn.mcfun.utils.OrderExecute;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import java.awt.Component;
import java.awt.Font;
import java.awt.Image;
import java.awt.LayoutManager;
import java.awt.Toolkit;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;

import static cn.mcfun.utils.AES.decryptWithAesCBC;

public class Main {
    private static Main main;
    private boolean running = true;
    private final ThreadPoolExecutor executor;
    private static Queue<Runnable> taskList = new LinkedList();
    private static Queue<Order> orders = new LinkedList();
    private static int CORE_POOL_SIZE = 30;
    private static String gachaId = "";
    private static String svtId = "";
    private static String type = "";
    private static String num = "";
    private static int flag = 0;
    private static final int MAX_POOL_SIZE = 1000;
    private static final int QUEUE_CAPACITY = 100000;
    private static final Long KEEP_ALIVE_TIME = 10L;
    public static String appVer = "";
    public static String assetbundleFolder = "";
    public static String dataServerFolderCrc = "";
    public static String dataVer = "";
    public static String dateVer = "";
    public static String storyAdjustIds = "";
    public static String animalName = "";
    public static byte[] key;
    public static byte[] iv;
    public static JSONArray json;

    public static void main(String[] args) {
        File file = new File("config.properties");
        if (!file.exists()) {
            try {
                file.createNewFile();
                FileWriter writer = new FileWriter(file, false);
                writer.append("#数据库连接地址\nurl=localhost\n#数据库端口\nport=3306\n#数据库用户名\nuser=root\n#数据库密码\npassword=\n#版本信息\nappVer=\n" +
                        "dataVer=\n" +
                        "dateVer=\n" +
                        "assetbundleFolder=\n" +
                        "animalName=kzdMtpmzqCHAfx00saU1gIhTjYCuOD1JstqtisXsGYqRVcqrHRydj3k6vJCySu3g\n");
                writer.flush();
                writer.close();
            } catch (IOException var14) {
                var14.printStackTrace();
            }
        }

        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (Exception var13) {
            System.out.println("初始化失败");
        }

        Properties props = new Properties();

        try {
            props.load(new FileInputStream("config.properties"));
        } catch (IOException var12) {
            var12.printStackTrace();
        }
        appVer = props.getProperty("appVer");
        assetbundleFolder = props.getProperty("assetbundleFolder");
        CRC32 crc32 = new CRC32();
        crc32.update(assetbundleFolder.getBytes(StandardCharsets.UTF_8));
        dataServerFolderCrc = String.valueOf(crc32.getValue());
        dataVer = props.getProperty("dataVer");
        dateVer = props.getProperty("dateVer");
        animalName = props.getProperty("animalName");
        byte[] a = Main.animalName.getBytes();
        key = new byte[32];
        for (int i=0;i<32; i++) {
            key[i] = (byte)(a[i] ^ 4);
        }
        iv = new byte[a.length-32];
        for (int i=0;i<a.length-32; i++) {
            iv[i] = (byte)(a[i+32] ^ 8);
        }
        JFrame frame = new JFrame("FGO日服抽卡");
        Image icon = Toolkit.getDefaultToolkit().getImage(Main.class.getClassLoader().getResource("logo.png"));
        frame.setIconImage(icon);
        frame.setSize(309, 550);
        frame.setLocationRelativeTo((Component)null);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(3);
        JPanel panel = new JPanel();
        frame.add(panel);
        placeComponents(panel);
        JButton logON = new JButton("显示控制台输出");
        logON.setFont(new Font("宋体", 0, 15));
        logON.setBounds(10, 335, 270, 30);
        panel.add(logON);
        JButton logOFF = new JButton("隐藏控制台输出");
        logOFF.setFont(new Font("宋体", 0, 15));
        logOFF.setBounds(10, 335, 270, 30);
        logOFF.setVisible(false);
        panel.add(logOFF);
        JTextArea console = new JTextArea();
        MyPrintStream mps = new MyPrintStream(System.out, console);
        System.setOut(mps);
        System.setErr(mps);
        console.setBounds(310, 20, 460, 460);
        console.setEnabled(false);
        console.setVisible(false);
        JScrollPane sp = new JScrollPane(console);
        sp.setBounds(310, 20, 460, 460);
        panel.add(sp);
        logON.addActionListener((e) -> {
            logON.setVisible(false);
            logOFF.setVisible(true);
            console.setVisible(true);
            frame.setSize(800, 550);
        });
        logOFF.addActionListener((e) -> {
            logON.setVisible(true);
            logOFF.setVisible(false);
            console.setVisible(false);
            frame.setSize(309, 550);
        });
        frame.setVisible(true);
    }

    private static void placeComponents(JPanel panel) {
        panel.setLayout((LayoutManager)null);
        JLabel poolSizeLabel = new JLabel("线程数量：");
        poolSizeLabel.setFont(new Font("宋体", 0, 15));
        poolSizeLabel.setBounds(10, 20, 120, 25);
        panel.add(poolSizeLabel);
        JComboBox<String> poolSizeJComboBox = new JComboBox();
        poolSizeJComboBox.addItem("20");
        poolSizeJComboBox.addItem("30");
        poolSizeJComboBox.addItem("40");
        poolSizeJComboBox.addItem("60");
        poolSizeJComboBox.addItem("100");
        poolSizeJComboBox.setBounds(130, 20, 150, 25);
        poolSizeJComboBox.setFont(new Font("宋体", 0, 15));
        panel.add(poolSizeJComboBox);
        JLabel gachaIdLabel = new JLabel("卡池：");
        gachaIdLabel.setFont(new Font("宋体", 0, 15));
        gachaIdLabel.setBounds(10, 60, 120, 25);
        panel.add(gachaIdLabel);
        JTextField gachaIdText = new JTextField(20);
        gachaIdText.setBounds(130, 60, 150, 25);
        gachaIdText.setFont(new Font("宋体", 0, 15));
        panel.add(gachaIdText);
        JLabel svtIdLabel = new JLabel("从者svtId：");
        svtIdLabel.setFont(new Font("宋体", 0, 15));
        svtIdLabel.setBounds(10, 100, 120, 25);
        panel.add(svtIdLabel);
        JTextField svtIdText = new JTextField(20);
        svtIdText.setFont(new Font("宋体", 0, 15));
        svtIdText.setBounds(130, 100, 150, 25);
        svtIdText.setDocument(new MyRegExp("\\d{0,8}"));
        panel.add(svtIdText);
        JLabel typeLabel = new JLabel("抽卡类型：");
        typeLabel.setFont(new Font("宋体", 0, 15));
        typeLabel.setBounds(10, 140, 120, 25);
        panel.add(typeLabel);
        JComboBox<String> typeJComboBox = new JComboBox();
        typeJComboBox.addItem("单抽");
        typeJComboBox.addItem("十连");
        typeJComboBox.setFont(new Font("宋体", 0, 15));
        typeJComboBox.setBounds(130, 140, 150, 25);
        panel.add(typeJComboBox);
        JLabel numLabel = new JLabel("宝具数量：");
        numLabel.setFont(new Font("宋体", 0, 15));
        numLabel.setBounds(10, 180, 120, 25);
        panel.add(numLabel);
        JComboBox<String> numJComboBox = new JComboBox();
        numJComboBox.addItem("0");
        numJComboBox.addItem("1");
        numJComboBox.addItem("2");
        numJComboBox.addItem("3");
        numJComboBox.addItem("4");
        numJComboBox.addItem("5");
        numJComboBox.setFont(new Font("宋体", 0, 15));
        numJComboBox.setBounds(130, 180, 150, 25);
        panel.add(numJComboBox);
        JButton startButton = new JButton("开始执行");
        startButton.setFont(new Font("宋体", 0, 15));
        startButton.setBounds(10, 220, 270, 50);
        panel.add(startButton);
        JButton stopButton = new JButton("强制停止");
        startButton.setFont(new Font("宋体", 0, 15));
        stopButton.setBounds(10, 285, 270, 50);
        stopButton.setEnabled(false);
        panel.add(stopButton);
        JButton setup = new JButton("获取版本信息");
        setup.setFont(new Font("宋体", 0, 15));
        setup.setBounds(10, 400, 270, 50);
        panel.add(setup);
        startButton.setEnabled(true);
        gachaIdText.setEnabled(true);
        svtIdText.setEnabled(true);
        poolSizeJComboBox.setEnabled(true);
        typeJComboBox.setEnabled(true);
        numJComboBox.setEnabled(true);
        setup.addActionListener((e) -> {
            String result = new GetRequest().sendGet("https://game.fate-go.jp/gamedata/top?appVer=2.44.0");
            JSONObject res = JSONObject.parseObject(result);
            if (res.getJSONArray("response").getJSONObject(0).getJSONObject("fail").getString("action") != null) {
                if (res.getJSONArray("response").getJSONObject(0).getJSONObject("fail").getString("action").equals("app_version_up")) {
                    String newVersion = res.getJSONArray("response").getJSONObject(0).getJSONObject("fail").getString("detail");
                    Pattern pattern = Pattern.compile(".*新ver.：(.*)、現.*");
                    Matcher matcher = pattern.matcher(newVersion);
                    if(matcher.find()){
                        appVer = matcher.group(1);
                        result = new GetRequest().sendGet("https://game.fate-go.jp/gamedata/top?appVer="+matcher.group(1));
                        res = JSONObject.parseObject(result);
                    }
                }
            }
            dataVer = res.getJSONArray("response").getJSONObject(0).getJSONObject("success").getString("dataVer");
            dateVer = res.getJSONArray("response").getJSONObject(0).getJSONObject("success").getString("dateVer");
            String assetbundle = res.getJSONArray("response").getJSONObject(0).getJSONObject("success").getString("assetbundle");
            Map<String,Object> map = mouseInfoMsgPack(Base64.getDecoder().decode(assetbundle));
            assetbundleFolder = (String) map.get("folderName");
            CRC32 crc32 = new CRC32();
            crc32.update(assetbundleFolder.getBytes(StandardCharsets.UTF_8));
            dataServerFolderCrc = String.valueOf(crc32.getValue());
            animalName = (String) map.get("animalName");
            byte[] a = Main.animalName.getBytes();
            key = new byte[32];
            for (int i=0;i<32; i++) {
                key[i] = (byte)(a[i] ^ 4);
            }
            iv = new byte[a.length-32];
            for (int i=0;i<a.length-32; i++) {
                iv[i] = (byte)(a[i+32] ^ 8);
            }
            Properties props = new Properties();

            try {
                props.load(new FileInputStream("config.properties"));
            } catch (IOException var12) {
                var12.printStackTrace();
            }
            try {
                props.setProperty("appVer",appVer);
                props.setProperty("dataVer",dataVer);
                props.setProperty("dateVer",dateVer);
                props.setProperty("assetbundleFolder",assetbundleFolder);
                props.setProperty("animalName",animalName);
                FileOutputStream oFile = new FileOutputStream("config.properties");
                props.store(oFile, "The New properties file");
                oFile.close();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
            setup.setEnabled(false);
            startButton.setEnabled(true);
            gachaIdText.setEnabled(true);
            svtIdText.setEnabled(true);
            poolSizeJComboBox.setEnabled(true);
            typeJComboBox.setEnabled(true);
            numJComboBox.setEnabled(true);
            setup.setText("获取版本信息完成");
            System.out.println("获取版本信息完成");
        });
        startButton.addActionListener((e) -> {
            if (!appVer.equals("")) {
                if (!gachaIdText.getText().equals("") && !svtIdText.getText().equals("")) {
                    startButton.setEnabled(false);
                    gachaIdText.setEnabled(false);
                    svtIdText.setEnabled(false);
                    poolSizeJComboBox.setEnabled(false);
                    typeJComboBox.setEnabled(false);
                    numJComboBox.setEnabled(false);
                    stopButton.setEnabled(true);
                    if (flag == 0) {
                        flag = 1;
                        Properties props = new Properties();

                        try {
                            props.load(new FileInputStream("config.properties"));
                        } catch (IOException var10) {
                            var10.printStackTrace();
                        }

                        System.out.println("开始执行任务！");
                        String mstGachaStoryAdjust = new GetRequest().sendGet("https://raw.fastgit.org/xiaoheimaoo/FGOData/master/JP/gamedata/mstGachaStoryAdjust.json");
                        JSONArray mstGachaStoryAdjustjson = JSONArray.parseArray(mstGachaStoryAdjust);
                        for(int i=0;i<mstGachaStoryAdjustjson.size();i++){
                            if(mstGachaStoryAdjustjson.getJSONObject(i).getString("gachaId").equals(gachaIdText.getText())){
                                if(storyAdjustIds.equals("")){
                                    storyAdjustIds = mstGachaStoryAdjustjson.getJSONObject(i).getString("adjustId");
                                }else{
                                    storyAdjustIds = storyAdjustIds+","+mstGachaStoryAdjustjson.getJSONObject(i).getString("adjustId");
                                }
                            }
                        }
                        if(storyAdjustIds.equals("")){
                            storyAdjustIds = "[]";
                        }else{
                            storyAdjustIds = "["+storyAdjustIds+"]";
                        }
                        (new Thread(() -> {
                            start(poolSizeJComboBox.getSelectedItem().toString(), gachaIdText.getText(), svtIdText.getText(), typeJComboBox.getSelectedItem().toString(), numJComboBox.getSelectedItem().toString());
                        })).start();
                    } else {
                        System.out.println("请勿重复执行！");
                    }
                } else {
                    System.out.println("卡池id和从者id不能为空！");
                }
            } else {
                System.out.println("请先获取版本信息！");
            }

        });
        stopButton.addActionListener((e) -> {
            System.exit(0);
        });
    }

    public Main() {
        this.executor = new ThreadPoolExecutor(CORE_POOL_SIZE, 1000, KEEP_ALIVE_TIME, TimeUnit.SECONDS, new ArrayBlockingQueue(100000), new CallerRunsPolicy());
    }

    public static void start(String arg1, String arg2, String arg3, String arg4, String arg5) {
        CORE_POOL_SIZE = Integer.valueOf(arg1);
        gachaId = arg2;
        svtId = arg3;
        type = arg4;
        num = arg5;

        try {
            if (type.equals("单抽")) {
                type = "1";
            } else {
                type = "10";
            }
            Connection conn2 = Hikari.getConnection();
            String sql2 = "update `order` set status='待执行',`current`=0,content=?,gachaId=?,type=?,svtId=? where status!='已完成' or status is null";
            PreparedStatement ps2 = conn2.prepareStatement(sql2);
            ps2.setString(1, num);
            ps2.setString(2, gachaId);
            ps2.setString(3, type);
            ps2.setString(4, svtId);
            ps2.executeUpdate();
            conn2.close();
            ps2.close();
        } catch (SQLException var15) {
            if (!var15.toString().contains("you can no longer use it")) {
                var15.printStackTrace();
            }
        }

        main = new Main();
        main.loadOrders();

        while(!orders.isEmpty()) {
            synchronized(orders) {
                main.executor.execute(new OrderExecute((Order)orders.poll()));
            }
        }

        while(main.isRunning()) {
            try {
                Thread.sleep(10L);
            } catch (InterruptedException var13) {
                var13.printStackTrace();
            }

            if (!orders.isEmpty()) {
                synchronized(orders) {
                    main.executor.execute(new OrderExecute((Order)orders.poll()));
                }
            }

            if (!taskList.isEmpty()) {
                synchronized(taskList) {
                    ((Runnable)taskList.poll()).run();
                }
            }
        }

    }

    public boolean isRunning() {
        return this.running;
    }

    public void loadOrders() {
        try {
            Connection conn = Hikari.getConnection();
            String sql = "update `order` set status='列队中' where status='待执行' or status='执行中'";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.executeUpdate();
            sql = "select * from `order` where status='列队中'";
            ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while(rs.next()) {
                Order order = new Order();
                order.setOrderId(rs.getString("order"));
                order.setUsername(rs.getString("user"));
                order.setPassword(rs.getString("pwd"));
                order.setContent(rs.getString("content"));
                order.setCurrent(rs.getString("current"));
                order.setGachaId(rs.getString("gachaId"));
                order.setSvtId(rs.getString("svtId"));
                addToOrderQueue(order);
            }

            conn.close();
            rs.close();
            ps.close();
        } catch (SQLException var6) {
            if (!var6.toString().contains("you can no longer use it")) {
                var6.printStackTrace();
            }
        }

    }

    public static void addToOrderQueue(Order order) {
        synchronized(orders) {
            orders.offer(order);
        }
    }
    public static Map<String,Object> mouseInfoMsgPack(byte[] data){
        byte[] InfoTop = new byte[32];
        byte[] array = new byte[data.length - 32];
        byte[] infoData = "W0Juh4cFJSYPkebJB9WpswNF51oa6Gm7".getBytes(StandardCharsets.UTF_8);
        System.arraycopy(data, 0, InfoTop, 0, 32);
        System.arraycopy(data, 32, array, 0, data.length - 32);
        return mouseHomeMsgPack(array, infoData, InfoTop);
    }
    public static Map<String,Object> mouseHomeMsgPack(byte[] data, byte[] home, byte[] info){
        Map<String,Object> a = decryptWithAesCBC(home,info,data);
        return a;
    }
}
