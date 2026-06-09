// ============================================================
// CHAI-LIAR v4.0 - COMPLETE FEATURE PACK
// All requested features implemented: Unique IDs, Shift Summaries,
// Free-Item Audit, Happy Hour, Unified Storage, Auto-Save, Backup,
// Individual Ledger Files, and Stream filtering.
// ============================================================

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.text.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

// ============================================================
// MAIN FRAME: Project1
// ============================================================
public class Project1 extends JFrame {
    public DashboardPanel dashboard;
    private MenuPanel menu;
    private OrderPanel orders;
    private HonestyPanel honesty;
    private LeaderboardPanel leaderboard;
    private ThemeManager themeManager;
    private TransactionLogPanel logPanel;
    private PersistenceManager persistence;
    private FeedbackSystem feedback;
    public SalesmanPanel salesmanPanel;
    private InventoryPanel inventory;
    public UserManager userManager;
    private AppSecurityManager securityManager;
    private SalesReportManager reportManager;
    public HappyHourManager happyHour;
    private JLabel userStatusLabel;

    // Data folder and backup
    public static final String DATA_DIR = "data";
    public static final String BACKUP_DIR = "data_backup";
    public static final String LEDGER_DIR = DATA_DIR + "/ledgers";

    public Project1() {
        setTitle("☕ CHAI-LIAR v4.0 | The Honesty Tea-st");
        setSize(1400, 900);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // --- Create data directories and backup ---
        createDataDirectories();
        backupExistingData();

        // --- Initialize Managers ---
        logPanel = new TransactionLogPanel();
        persistence = new PersistenceManager();
        feedback = new FeedbackSystem();
        userManager = new UserManager();
        securityManager = new AppSecurityManager();
        reportManager = new SalesReportManager();
        happyHour = new HappyHourManager();

        // --- Initialize Panels ---
        dashboard = new DashboardPanel(persistence, reportManager);
        menu = new MenuPanel(dashboard, logPanel, securityManager, reportManager, userManager, happyHour);
        orders = new OrderPanel(logPanel);
        honesty = new HonestyPanel(dashboard, logPanel, userManager, reportManager, securityManager);
        leaderboard = new LeaderboardPanel();
        themeManager = new ThemeManager(this);
        salesmanPanel = new SalesmanPanel(reportManager, securityManager, userManager);
        inventory = new InventoryPanel(menu, logPanel);

        // --- Login First ---
        if (!showLoginDialog()) {
            System.exit(0);
        }

        // --- Layout Setup (same as before) ---
        add(dashboard, BorderLayout.WEST);
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(menu, BorderLayout.CENTER);
        centerPanel.add(inventory, BorderLayout.SOUTH);
        add(centerPanel, BorderLayout.CENTER);
        add(orders, BorderLayout.EAST);
        add(honesty, BorderLayout.SOUTH);
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(honesty, BorderLayout.NORTH);
        southPanel.add(logPanel, BorderLayout.SOUTH);
        add(southPanel, BorderLayout.SOUTH);
        JPanel northPanel = new JPanel(new BorderLayout());
        JPanel topPanels = new JPanel(new GridLayout(1, 2, 10, 10));
        topPanels.add(leaderboard);
        topPanels.add(salesmanPanel);
        northPanel.add(topPanels, BorderLayout.CENTER);
        User current = userManager.getCurrentUser();
        String username = (current != null) ? current.getUsername() : "Guest";
        String role = (current != null) ? current.getRole() : "None";
        userStatusLabel = new JLabel(" 👤 " + username + " | Role: " + role + " ");
        userStatusLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        userStatusLabel.setBorder(BorderFactory.createEtchedBorder());
        userStatusLabel.setOpaque(true);
        userStatusLabel.setBackground(new Color(220, 240, 255));
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        statusPanel.add(userStatusLabel);
        northPanel.add(statusPanel, BorderLayout.NORTH);
        add(northPanel, BorderLayout.NORTH);
        add(logPanel, BorderLayout.SOUTH);

        // --- Load saved data ---
        persistence.loadData(dashboard);
        reportManager.loadReports();
        menu.loadSavedMenuItems();
        happyHour.load();

        // --- Start feedback timer ---
        feedback.attachToDashboard(dashboard, logPanel);
        securityManager.logAction("Application started", userManager.getCurrentUser());

        // --- Add window listener for auto-save ---
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveAll();
                System.exit(0);
            }
        });
    }

    private void createDataDirectories() {
        try {
            Files.createDirectories(Paths.get(DATA_DIR));
            Files.createDirectories(Paths.get(LEDGER_DIR));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void backupExistingData() {
        File dataDir = new File(DATA_DIR);
        if (dataDir.exists() && dataDir.isDirectory()) {
            // Create timestamped backup folder
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            File backup = new File(BACKUP_DIR + "_" + timestamp);
            try {
                copyDirectory(dataDir.toPath(), backup.toPath());
                System.out.println("Backup created: " + backup.getPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        Files.walk(source).forEach(src -> {
            try {
                Path dest = target.resolve(source.relativize(src));
                if (Files.isDirectory(src))
                    Files.createDirectories(dest);
                else
                    Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void saveAll() {
        persistence.saveData(dashboard.getTotalSales(), dashboard.getHonestyScore());
        userManager.saveUsers();
        menu.saveMenuItems();
        reportManager.saveReports();
        happyHour.save();
        logPanel.addEntry("Application closed – all data saved.");
    }

    private boolean showLoginDialog() {
        JDialog loginDialog = new JDialog(this, "Login - CHAI-LIAR", true);
        loginDialog.setLayout(new BorderLayout());
        loginDialog.setSize(400, 300);
        loginDialog.setLocationRelativeTo(this);
        JPanel loginPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        JTextField usernameField = new JTextField(15);
        JPasswordField passwordField = new JPasswordField(15);
        JComboBox<String> roleCombo = new JComboBox<>(new String[]{"OWNER", "SALESMAN"});
        gbc.gridx = 0; gbc.gridy = 0;
        loginPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        loginPanel.add(usernameField, gbc);
        gbc.gridx = 0; gbc.gridy = 1;
        loginPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        loginPanel.add(passwordField, gbc);
        gbc.gridx = 0; gbc.gridy = 2;
        loginPanel.add(new JLabel("Role:"), gbc);
        gbc.gridx = 1;
        loginPanel.add(roleCombo, gbc);
        JButton loginBtn = new JButton("Login");
        JButton registerBtn = new JButton("Register New User");
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(loginBtn);
        buttonPanel.add(registerBtn);
        loginDialog.add(loginPanel, BorderLayout.CENTER);
        loginDialog.add(buttonPanel, BorderLayout.SOUTH);
        final boolean[] loginSuccess = {false};
        loginBtn.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            String role = (String) roleCombo.getSelectedItem();
            User user = userManager.authenticate(username, password, role);
            if (user != null) {
                loginSuccess[0] = true;
                loginDialog.dispose();
            } else {
                JOptionPane.showMessageDialog(loginDialog,
                        "Invalid credentials or user not found!",
                        "Login Failed", JOptionPane.ERROR_MESSAGE);
            }
        });
        registerBtn.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            String role = (String) roleCombo.getSelectedItem();
            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(loginDialog,
                        "Username and password cannot be empty!",
                        "Registration Failed", JOptionPane.ERROR_MESSAGE);
                return;
            }
            User newUser = new User(username, password, role);
            if (userManager.registerUser(newUser)) {
                JOptionPane.showMessageDialog(loginDialog,
                        "User registered successfully! Please login.",
                        "Registration Successful", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(loginDialog,
                        "Username already exists!",
                        "Registration Failed", JOptionPane.ERROR_MESSAGE);
            }
        });
        loginDialog.setVisible(true);
        return loginSuccess[0];
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new Project1().setVisible(true);
        });
    }
}

// ============================================================
// UTILITY METHOD FOR STRING REPETITION (Java 8 compatible)
// ============================================================
class StringUtils {
    public static String repeat(String str, int count) {
        if (count <= 0) return "";
        StringBuilder sb = new StringBuilder(str.length() * count);
        for (int i = 0; i < count; i++) sb.append(str);
        return sb.toString();
    }
}

// ============================================================
// USER CLASS
// ============================================================
class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private String username;
    private String password;
    private String role;
    private String salesmanId; // only for SALESMAN role
    private Date registrationDate;

    public User(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.registrationDate = new Date();
        if (role.equals("SALESMAN")) {
            this.salesmanId = UUID.randomUUID().toString();
        }
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
    public String getSalesmanId() { return salesmanId; }
    public Date getRegistrationDate() { return registrationDate; }

    public boolean hasPermission(String action) {
        if (role.equals("OWNER")) return true;
        if (role.equals("SALESMAN")) {
            return action.equals("SELL") || action.equals("VIEW_MENU") ||
                   action.equals("VIEW_SALES_REPORT");
        }
        return false;
    }
}

// ============================================================
// USER MANAGER
// ============================================================
class UserManager {
    private Map<String, User> users;
    private User currentUser;
    private File userFile;

    public UserManager() {
        users = new HashMap<>();
        userFile = new File(Project1.DATA_DIR + "/users.dat");
        loadUsers();
        if (users.isEmpty()) {
            User admin = new User("admin", "admin123", "OWNER");
            users.put(admin.getUsername(), admin);
            saveUsers();
        }
    }

    public boolean registerUser(User user) {
        if (users.containsKey(user.getUsername())) return false;
        users.put(user.getUsername(), user);
        saveUsers();
        return true;
    }

    public User authenticate(String username, String password, String role) {
        User user = users.get(username);
        if (user != null && user.getPassword().equals(password) && user.getRole().equals(role)) {
            currentUser = user;
            return user;
        }
        return null;
    }

    public User getCurrentUser() { return currentUser; }

    public void saveUsers() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(userFile))) {
            oos.writeObject(users);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @SuppressWarnings("unchecked")
    private void loadUsers() {
        if (!userFile.exists()) return;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(userFile))) {
            users = (Map<String, User>) ois.readObject();
        } catch (Exception e) { e.printStackTrace(); }
    }
}

// ============================================================
// SECURITY MANAGER
// ============================================================
class AppSecurityManager {
    private java.util.List<String> auditLog;
    private File auditFile;

    public AppSecurityManager() {
        auditLog = new ArrayList<>();
        auditFile = new File(Project1.DATA_DIR + "/audit.log");
        loadAuditLog();
    }

    public void logAction(String action, User user) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String logEntry = String.format("[%s] User: %s (Role: %s) - %s",
                timestamp, user.getUsername(), user.getRole(), action);
        auditLog.add(logEntry);
        saveAuditLog();
    }

    public boolean checkPermission(User user, String action) {
        if (user == null) return false;
        boolean hasPermission = user.hasPermission(action);
        if (!hasPermission) logAction("DENIED: " + action + " (Insufficient permissions)", user);
        return hasPermission;
    }

    public java.util.List<String> getAuditLog() { return new ArrayList<>(auditLog); }

    private void saveAuditLog() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(auditFile, true))) {
            if (!auditLog.isEmpty()) pw.println(auditLog.get(auditLog.size() - 1));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadAuditLog() {
        if (!auditFile.exists()) return;
        try (Scanner sc = new Scanner(auditFile)) {
            while (sc.hasNextLine()) auditLog.add(sc.nextLine());
        } catch (Exception e) { e.printStackTrace(); }
    }
}

// ============================================================
// MENU ITEM CLASS
// ============================================================
class MenuItem implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private int price;
    private String category;
    private boolean available;
    private Date addedDate;
    private String addedBy;

    public MenuItem(String name, int price, String category, String addedBy) {
        this.name = name;
        this.price = price;
        this.category = category;
        this.available = true;
        this.addedDate = new Date();
        this.addedBy = addedBy;
    }

    public String getName() { return name; }
    public int getPrice() { return price; }
    public String getCategory() { return category; }
    public boolean isAvailable() { return available; }
    public Date getAddedDate() { return addedDate; }
    public String getAddedBy() { return addedBy; }

    public void setPrice(int price) { this.price = price; }
    public void setAvailable(boolean available) { this.available = available; }
    public void setName(String name) { this.name = name; }

    @Override
    public String toString() {
        return String.format("%s - Rs.%d (%s)", name, price, available ? "Available" : "Unavailable");
    }
}

// ============================================================
// SALE TRANSACTION CLASS (now includes salesmanId and free flag)
// ============================================================
class SaleTransaction implements Serializable {
    private static final long serialVersionUID = 1L;
    private String itemName;
    private int originalPrice;
    private int finalPrice; // price after discount (if any)
    private String salesmanId; // unique ID of salesman
    private String soldBy; // username of user who recorded sale
    private Date timestamp;
    private boolean isFree;

    public SaleTransaction(String itemName, int originalPrice, int finalPrice, String salesmanId, String soldBy, boolean isFree) {
        this.itemName = itemName;
        this.originalPrice = originalPrice;
        this.finalPrice = finalPrice;
        this.salesmanId = salesmanId;
        this.soldBy = soldBy;
        this.timestamp = new Date();
        this.isFree = isFree;
    }

    public String getItemName() { return itemName; }
    public int getOriginalPrice() { return originalPrice; }
    public int getFinalPrice() { return finalPrice; }
    public String getSalesmanId() { return salesmanId; }
    public String getSoldBy() { return soldBy; }
    public Date getTimestamp() { return timestamp; }
    public boolean isFree() { return isFree; }
}

// ============================================================
// HAPPY HOUR MANAGER
// ============================================================
class HappyHourManager {
    private LocalTime startTime;
    private LocalTime endTime;
    private int discountPercent;
    private boolean enabled;
    private File configFile;

    public HappyHourManager() {
        configFile = new File(Project1.DATA_DIR + "/happyhour.dat");
        load();
    }

    public void setHappyHour(LocalTime start, LocalTime end, int discount) {
        this.startTime = start;
        this.endTime = end;
        this.discountPercent = Math.min(100, Math.max(0, discount));
        this.enabled = true;
        save();
    }

    public void disable() {
        this.enabled = false;
        save();
    }

    public boolean isHappyHour() {
        if (!enabled) return false;
        LocalTime now = LocalTime.now();
        return now.isAfter(startTime) && now.isBefore(endTime);
    }

    public int applyDiscount(int price) {
        if (!isHappyHour()) return price;
        return price * (100 - discountPercent) / 100;
    }

    public String getInfo() {
        if (!enabled) return "Happy Hour: Disabled";
        return String.format("Happy Hour: %s - %s (%d%% off)",
                startTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                endTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                discountPercent);
    }

    public void save() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(configFile))) {
            oos.writeObject(new Object[]{startTime, endTime, discountPercent, enabled});
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void load() {
        if (!configFile.exists()) {
            enabled = false;
            return;
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(configFile))) {
            Object[] data = (Object[]) ois.readObject();
            startTime = (LocalTime) data[0];
            endTime = (LocalTime) data[1];
            discountPercent = (int) data[2];
            enabled = (boolean) data[3];
        } catch (Exception e) { e.printStackTrace(); }
    }
}

// ============================================================
// SALES REPORT MANAGER (enhanced with ledger and free audit)
// ============================================================
class SalesReportManager {
    private java.util.List<SaleTransaction> sales;
    private File salesFile;
    private Map<String, java.util.List<SaleTransaction>> salesmanSales;
    private Map<String, java.util.List<SaleTransaction>> itemSales;
    private java.util.List<FreeItemLog> freeLogs;

    public SalesReportManager() {
        sales = new ArrayList<>();
        salesmanSales = new HashMap<>();
        itemSales = new HashMap<>();
        freeLogs = new ArrayList<>();
        salesFile = new File(Project1.DATA_DIR + "/sales_reports.dat");
        loadReports();
    }

    public void recordSale(String itemName, int originalPrice, int finalPrice, String salesmanId, User user, boolean isFree) {
        SaleTransaction transaction = new SaleTransaction(itemName, originalPrice, finalPrice, salesmanId, user.getUsername(), isFree);
        sales.add(transaction);
        salesmanSales.computeIfAbsent(salesmanId, k -> new ArrayList<>()).add(transaction);
        itemSales.computeIfAbsent(itemName, k -> new ArrayList<>()).add(transaction);
        if (isFree) {
            freeLogs.add(new FreeItemLog(itemName, salesmanId, user.getUsername(), new Date()));
        }
        // Write to individual ledger
        writeToLedger(transaction);
        saveReports();
    }

    private void writeToLedger(SaleTransaction t) {
        String date = new SimpleDateFormat("yyyy-MM-dd").format(t.getTimestamp());
        String fileName = Project1.LEDGER_DIR + "/" + t.getSalesmanId() + "_" + date + ".dat";
        try (PrintWriter pw = new PrintWriter(new FileWriter(fileName, true))) {
            pw.printf("%s | %s | %d | %d | %s%n",
                    new SimpleDateFormat("HH:mm:ss").format(t.getTimestamp()),
                    t.getItemName(),
                    t.getOriginalPrice(),
                    t.getFinalPrice(),
                    t.isFree() ? "FREE" : "SALE");
        } catch (Exception e) { e.printStackTrace(); }
    }

    public java.util.List<SaleTransaction> getSalesBySalesmanId(String salesmanId) {
        return salesmanSales.getOrDefault(salesmanId, new ArrayList<>());
    }

    public java.util.List<SaleTransaction> getSalesByItem(String itemName) {
        return itemSales.getOrDefault(itemName, new ArrayList<>());
    }

    public Map<String, Integer> getTopSellingItems() {
        Map<String, Integer> itemCounts = new HashMap<>();
        for (SaleTransaction sale : sales) {
            itemCounts.put(sale.getItemName(), itemCounts.getOrDefault(sale.getItemName(), 0) + 1);
        }
        return itemCounts;
    }

    public Map<String, Integer> getSalesmanTotal() {
        Map<String, Integer> salesmanTotal = new HashMap<>();
        for (SaleTransaction sale : sales) {
            salesmanTotal.put(sale.getSalesmanId(),
                    salesmanTotal.getOrDefault(sale.getSalesmanId(), 0) + sale.getFinalPrice());
        }
        return salesmanTotal;
    }

    public java.util.List<FreeItemLog> getFreeLogs() { return new ArrayList<>(freeLogs); }

    public String generateFullReport() {
        StringBuilder report = new StringBuilder();
        report.append(StringUtils.repeat("=", 60)).append("\n");
        report.append("COMPLETE SALES REPORT\n");
        report.append("Generated: ").append(new Date()).append("\n");
        report.append(StringUtils.repeat("=", 60)).append("\n\n");
        report.append("SALES BY SALESMAN ID:\n");
        report.append(StringUtils.repeat("-", 40)).append("\n");
        Map<String, Integer> salesmanTotal = getSalesmanTotal();
        for (Map.Entry<String, Integer> entry : salesmanTotal.entrySet()) {
            report.append(String.format("%-20s: Rs.%,8d (%d sales)\n",
                    entry.getKey(), entry.getValue(),
                    getSalesBySalesmanId(entry.getKey()).size()));
        }
        report.append("\nTOP SELLING ITEMS:\n");
        report.append(StringUtils.repeat("-", 40)).append("\n");
        Map<String, Integer> topItems = getTopSellingItems();
        topItems.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .forEach(entry -> report.append(String.format("%-20s: %d sales\n",
                        entry.getKey(), entry.getValue())));
        report.append("\nTOTAL SALES SUMMARY:\n");
        report.append(StringUtils.repeat("-", 40)).append("\n");
        int totalRevenue = sales.stream().mapToInt(SaleTransaction::getFinalPrice).sum();
        report.append(String.format("Total Transactions: %d\n", sales.size()));
        report.append(String.format("Total Revenue: Rs.%,d\n", totalRevenue));
        report.append(String.format("Average Sale Value: Rs.%,.2f\n",
                sales.isEmpty() ? 0 : totalRevenue / (double) sales.size()));
        return report.toString();
    }

    public void saveReports() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(salesFile))) {
            oos.writeObject(sales);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @SuppressWarnings("unchecked")
    public void loadReports() {
        if (!salesFile.exists()) return;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(salesFile))) {
            sales = (java.util.List<SaleTransaction>) ois.readObject();
            for (SaleTransaction sale : sales) {
                salesmanSales.computeIfAbsent(sale.getSalesmanId(), k -> new ArrayList<>()).add(sale);
                itemSales.computeIfAbsent(sale.getItemName(), k -> new ArrayList<>()).add(sale);
                if (sale.isFree()) {
                    freeLogs.add(new FreeItemLog(sale.getItemName(), sale.getSalesmanId(), sale.getSoldBy(), sale.getTimestamp()));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}

// ============================================================
// FREE ITEM LOG (for audit)
// ============================================================
class FreeItemLog implements Serializable {
    private static final long serialVersionUID = 1L;
    private String itemName;
    private String salesmanId;
    private String authorizedBy;
    private Date timestamp;

    public FreeItemLog(String itemName, String salesmanId, String authorizedBy, Date timestamp) {
        this.itemName = itemName;
        this.salesmanId = salesmanId;
        this.authorizedBy = authorizedBy;
        this.timestamp = timestamp;
    }

    public String getItemName() { return itemName; }
    public String getSalesmanId() { return salesmanId; }
    public String getAuthorizedBy() { return authorizedBy; }
    public Date getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return String.format("%s | Salesman: %s | Free: %s | Authorized by: %s",
                sdf.format(timestamp), salesmanId, itemName, authorizedBy);
    }
}

// ============================================================
// DASHBOARD PANEL
// ============================================================
class DashboardPanel extends JPanel {
    private JLabel salesLabel;
    private JProgressBar honestyBar;
    private JLabel statusLabel;
    private int totalSales;
    private int honestyScore;
    private PersistenceManager persistence;
    private SalesReportManager reportMgr;

    public DashboardPanel(PersistenceManager persistence, SalesReportManager reportMgr) {
        this.persistence = persistence;
        this.reportMgr = reportMgr;
        this.totalSales = 0;
        this.honestyScore = 75;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        setBackground(new Color(230, 220, 200));
        setPreferredSize(new Dimension(280, 0));

        salesLabel = new JLabel("Total Sales: Rs. " + totalSales);
        salesLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        honestyBar = new JProgressBar(0, 100);
        honestyBar.setValue(honestyScore);
        honestyBar.setStringPainted(true);
        statusLabel = new JLabel("Status: Honest Chaiwala");
        statusLabel.setFont(new Font("SansSerif", Font.ITALIC, 14));

        add(salesLabel);
        add(Box.createVerticalStrut(10));
        add(new JLabel("Honesty Meter:"));
        add(honestyBar);
        add(Box.createVerticalStrut(10));
        add(statusLabel);

        JButton reportBtn = new JButton("📊 View Full Report");
        reportBtn.addActionListener(e -> showFullReport());
        add(Box.createVerticalStrut(10));
        add(reportBtn);
    }

    public void addSale(int amount) {
        totalSales += amount;
        salesLabel.setText("Total Sales: Rs. " + totalSales);
        persistence.saveData(totalSales, honestyScore);
    }

    public void adjustHonesty(int points) {
        honestyScore = Math.max(0, Math.min(100, honestyScore + points));
        honestyBar.setValue(honestyScore);
        updateStatus();
        persistence.saveData(totalSales, honestyScore);
    }

    private void updateStatus() {
        if (honestyScore >= 70) {
            statusLabel.setText("Status: Honest Chaiwala");
            honestyBar.setForeground(new Color(34, 139, 34));
        } else if (honestyScore >= 40) {
            statusLabel.setText("Status: Somewhat Shady");
            honestyBar.setForeground(Color.ORANGE);
        } else {
            statusLabel.setText("Status: Certified Liar!");
            honestyBar.setForeground(Color.RED);
        }
    }

    private void showFullReport() {
        String report = reportMgr.generateFullReport();
        JTextArea textArea = new JTextArea(report);
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(600, 500));
        JOptionPane.showMessageDialog(this, scrollPane, "Complete Sales Report",
                JOptionPane.INFORMATION_MESSAGE);
    }

    public int getTotalSales() { return totalSales; }
    public int getHonestyScore() { return honestyScore; }
}

// ============================================================
// MENU PANEL (with Happy Hour integration)
// ============================================================
class MenuPanel extends JPanel {
    private DashboardPanel dashboard;
    private TransactionLogPanel log;
    private AppSecurityManager securityManager;
    private SalesReportManager reportManager;
    private UserManager userManager;
    private HappyHourManager happyHour;
    private Map<String, MenuItem> menuItems;
    private JPanel menuGrid;
    private JComboBox<String> categoryFilter;

    public MenuPanel(DashboardPanel dashboard, TransactionLogPanel log,
                     AppSecurityManager securityManager, SalesReportManager reportManager,
                     UserManager userManager, HappyHourManager happyHour) {
        this.dashboard = dashboard;
        this.log = log;
        this.securityManager = securityManager;
        this.reportManager = reportManager;
        this.userManager = userManager;
        this.happyHour = happyHour;
        this.menuItems = new LinkedHashMap<>();

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("MENU"));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addItemBtn = new JButton("➕ Add Item");
        addItemBtn.addActionListener(e -> addMenuItem());
        JButton removeItemBtn = new JButton("❌ Remove Item");
        removeItemBtn.addActionListener(e -> removeMenuItem());
        JButton editItemBtn = new JButton("✏️ Edit Item");
        editItemBtn.addActionListener(e -> editMenuItem());
        JButton viewReportBtn = new JButton("📊 Item Report");
        viewReportBtn.addActionListener(e -> showItemReport());
        categoryFilter = new JComboBox<>(new String[]{"All", "Beverages", "Snacks", "Custom"});
        categoryFilter.addActionListener(e -> refreshMenuDisplay());

        toolbar.add(addItemBtn);
        toolbar.add(removeItemBtn);
        toolbar.add(editItemBtn);
        toolbar.add(viewReportBtn);
        toolbar.add(new JLabel("Filter:"));
        toolbar.add(categoryFilter);
        add(toolbar, BorderLayout.NORTH);

        menuGrid = new JPanel(new GridLayout(0, 2, 10, 10));
        menuGrid.setBorder(new EmptyBorder(10, 10, 10, 10));
        add(new JScrollPane(menuGrid), BorderLayout.CENTER);

        addDefaultItems();
    }

    private void addDefaultItems() {
        addMenuItemToMap(new MenuItem("Masala Chai", 25, "Beverages", "System"));
        addMenuItemToMap(new MenuItem("Ginger Chai", 30, "Beverages", "System"));
        addMenuItemToMap(new MenuItem("Elaichi Chai", 30, "Beverages", "System"));
        addMenuItemToMap(new MenuItem("Cold Coffee", 55, "Beverages", "System"));
        addMenuItemToMap(new MenuItem("Samosa", 15, "Snacks", "System"));
        addMenuItemToMap(new MenuItem("Biscuits", 10, "Snacks", "System"));
        addMenuItemToMap(new MenuItem("Khari", 12, "Snacks", "System"));
        addMenuItemToMap(new MenuItem("Cake Slice", 40, "Snacks", "System"));
    }

    private void addMenuItemToMap(MenuItem item) {
        menuItems.put(item.getName(), item);
        saveMenuItems();
        refreshMenuDisplay();
    }

    private void addMenuItem() {
        User currentUser = userManager.getCurrentUser();
        if (!securityManager.checkPermission(currentUser, "MANAGE_MENU")) {
            JOptionPane.showMessageDialog(this,
                    "Only owners can add menu items!",
                    "Permission Denied", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // ... (same as before) ...
        JTextField nameField = new JTextField();
        JTextField priceField = new JTextField();
        JComboBox<String> categoryCombo = new JComboBox<>(new String[]{"Beverages", "Snacks", "Custom"});
        JTextField customCategory = new JTextField();
        customCategory.setEnabled(false);
        categoryCombo.addActionListener(e -> customCategory.setEnabled(categoryCombo.getSelectedItem().equals("Custom")));
        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
        panel.add(new JLabel("Item Name:")); panel.add(nameField);
        panel.add(new JLabel("Price (Rs.):")); panel.add(priceField);
        panel.add(new JLabel("Category:")); panel.add(categoryCombo);
        panel.add(new JLabel("Custom Category:")); panel.add(customCategory);
        int result = JOptionPane.showConfirmDialog(this, panel, "Add New Menu Item", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                String name = nameField.getText().trim();
                int price = Integer.parseInt(priceField.getText().trim());
                String category = categoryCombo.getSelectedItem().equals("Custom") ?
                        customCategory.getText().trim() : (String) categoryCombo.getSelectedItem();
                if (name.isEmpty() || price <= 0 || category.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Invalid input!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                MenuItem newItem = new MenuItem(name, price, category, currentUser.getUsername());
                addMenuItemToMap(newItem);
                securityManager.logAction("Added menu item: " + name, currentUser);
                log.addEntry("Menu item added: " + name + " (Rs." + price + ") by " + currentUser.getUsername());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid price!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void removeMenuItem() {
        User currentUser = userManager.getCurrentUser();
        if (!securityManager.checkPermission(currentUser, "MANAGE_MENU")) {
            JOptionPane.showMessageDialog(this,
                    "Only owners can remove menu items!",
                    "Permission Denied", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String[] items = menuItems.keySet().toArray(new String[0]);
        if (items.length == 0) {
            JOptionPane.showMessageDialog(this, "No items to remove!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String selected = (String) JOptionPane.showInputDialog(this,
                "Select item to remove:", "Remove Menu Item",
                JOptionPane.QUESTION_MESSAGE, null, items, items[0]);
        if (selected != null) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to remove " + selected + "?",
                    "Confirm Removal", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                menuItems.remove(selected);
                saveMenuItems();
                refreshMenuDisplay();
                securityManager.logAction("Removed menu item: " + selected, currentUser);
                log.addEntry("Menu item removed: " + selected + " by " + currentUser.getUsername());
            }
        }
    }

    private void editMenuItem() {
        User currentUser = userManager.getCurrentUser();
        if (!securityManager.checkPermission(currentUser, "MANAGE_MENU")) {
            JOptionPane.showMessageDialog(this,
                    "Only owners can edit menu items!",
                    "Permission Denied", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String[] items = menuItems.keySet().toArray(new String[0]);
        if (items.length == 0) {
            JOptionPane.showMessageDialog(this, "No items to edit!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String selected = (String) JOptionPane.showInputDialog(this,
                "Select item to edit:", "Edit Menu Item",
                JOptionPane.QUESTION_MESSAGE, null, items, items[0]);
        if (selected != null) {
            MenuItem item = menuItems.get(selected);
            JTextField nameField = new JTextField(item.getName());
            JTextField priceField = new JTextField(String.valueOf(item.getPrice()));
            JCheckBox availableCheck = new JCheckBox("Available", item.isAvailable());
            JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
            panel.add(new JLabel("Item Name:")); panel.add(nameField);
            panel.add(new JLabel("Price (Rs.):")); panel.add(priceField);
            panel.add(new JLabel("Status:")); panel.add(availableCheck);
            int result = JOptionPane.showConfirmDialog(this, panel, "Edit Menu Item", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                try {
                    String newName = nameField.getText().trim();
                    int newPrice = Integer.parseInt(priceField.getText().trim());
                    if (newName.isEmpty() || newPrice <= 0) {
                        JOptionPane.showMessageDialog(this, "Invalid input!", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    if (!newName.equals(selected)) menuItems.remove(selected);
                    item.setName(newName);
                    item.setPrice(newPrice);
                    item.setAvailable(availableCheck.isSelected());
                    menuItems.put(newName, item);
                    saveMenuItems();
                    refreshMenuDisplay();
                    securityManager.logAction("Edited menu item: " + selected + " -> " + newName, currentUser);
                    log.addEntry("Menu item edited: " + selected + " by " + currentUser.getUsername());
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid price!", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void showItemReport() {
        Map<String, Integer> itemSales = reportManager.getTopSellingItems();
        StringBuilder report = new StringBuilder();
        report.append("ITEM SALES REPORT\n");
        report.append(StringUtils.repeat("=", 50)).append("\n\n");
        for (Map.Entry<String, MenuItem> entry : menuItems.entrySet()) {
            MenuItem item = entry.getValue();
            int salesCount = itemSales.getOrDefault(item.getName(), 0);
            int revenue = salesCount * item.getPrice();
            report.append(String.format("%s\n", item.getName()));
            report.append(String.format("  Price: Rs.%d | Status: %s | Category: %s\n",
                    item.getPrice(), item.isAvailable() ? "Available" : "Unavailable", item.getCategory()));
            report.append(String.format("  Sales: %d units | Revenue: Rs.%,d\n", salesCount, revenue));
            report.append(String.format("  Added: %s by %s\n\n",
                    new SimpleDateFormat("dd-MM-yyyy").format(item.getAddedDate()), item.getAddedBy()));
        }
        JTextArea textArea = new JTextArea(report.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 600));
        JOptionPane.showMessageDialog(this, scrollPane, "Item Report", JOptionPane.INFORMATION_MESSAGE);
    }

    private void refreshMenuDisplay() {
        menuGrid.removeAll();
        String filter = (String) categoryFilter.getSelectedItem();
        for (MenuItem item : menuItems.values()) {
            if (!filter.equals("All") && !item.getCategory().equals(filter) &&
                    !(filter.equals("Custom") && !item.getCategory().equals("Beverages") && !item.getCategory().equals("Snacks")))
                continue;
            if (item.isAvailable()) addMenuItemButton(item);
            else addUnavailableItemLabel(item);
        }
        menuGrid.revalidate();
        menuGrid.repaint();
    }

    private void addMenuItemButton(MenuItem item) {
        JButton btn = new JButton("<html><b>" + item.getName() + "</b><br>" +
                "<font color='gray'>Rs. " + item.getPrice() + "</font><br>" +
                "<font size='2'>" + item.getCategory() + "</font></html>");
        btn.setFocusPainted(false);
        btn.addActionListener(e -> sellItem(item));
        menuGrid.add(btn);
    }

    private void addUnavailableItemLabel(MenuItem item) {
        JLabel label = new JLabel("<html><b>" + item.getName() + "</b><br>" +
                "<font color='gray'>Rs. " + item.getPrice() + "</font><br>" +
                "<font color='red'>(Unavailable)</font></html>");
        label.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setOpaque(true);
        label.setBackground(Color.LIGHT_GRAY);
        menuGrid.add(label);
    }

    private void sellItem(MenuItem item) {
        User currentUser = userManager.getCurrentUser();
        if (!securityManager.checkPermission(currentUser, "SELL")) {
            JOptionPane.showMessageDialog(this,
                    "You don't have permission to sell items!",
                    "Permission Denied", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Get salesman ID
        String salesmanId;
        if (currentUser.getRole().equals("SALESMAN")) {
            salesmanId = currentUser.getSalesmanId();
        } else {
            // Owner selling: prompt for salesman ID
            String input = JOptionPane.showInputDialog(this, "Enter salesman ID (or name):");
            if (input == null || input.trim().isEmpty()) return;
            salesmanId = input.trim();
        }

        // Apply happy hour discount
        int finalPrice = happyHour.applyDiscount(item.getPrice());
        boolean isFree = (finalPrice == 0); // zero price means free (honesty action may set price to 0)
        dashboard.addSale(finalPrice);
        reportManager.recordSale(item.getName(), item.getPrice(), finalPrice, salesmanId, currentUser, isFree);
        securityManager.logAction("Sold: " + item.getName() + " for Rs." + finalPrice + (isFree ? " (FREE)" : ""), currentUser);
        log.addEntry("Sold: " + item.getName() + " for Rs." + finalPrice +
                " | Salesman ID: " + salesmanId + " | Sold by: " + currentUser.getUsername());

        // Update salesman panel (if we have a Salesman object for that ID)
        Project1 mainFrame = (Project1) SwingUtilities.getWindowAncestor(this);
        mainFrame.salesmanPanel.recordSaleById(salesmanId, finalPrice);
        mainFrame.salesmanPanel.sortBySales();

        JOptionPane.showMessageDialog(this,
                "Sold " + item.getName() + " for Rs." + finalPrice + "!\n" +
                        (happyHour.isHappyHour() ? "Happy Hour discount applied!" : ""),
                "Sale Complete", JOptionPane.INFORMATION_MESSAGE);
    }

    public void loadSavedMenuItems() {
        File menuFile = new File(Project1.DATA_DIR + "/menu_items.dat");
        if (menuFile.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(menuFile))) {
                menuItems = (Map<String, MenuItem>) ois.readObject();
                refreshMenuDisplay();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    public void saveMenuItems() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(Project1.DATA_DIR + "/menu_items.dat"))) {
            oos.writeObject(menuItems);
        } catch (Exception e) { e.printStackTrace(); }
    }
}

// ============================================================
// SALESMAN PANEL (enhanced with shift summary and free log)
// ============================================================
class SalesmanPanel extends JPanel {
    private DefaultListModel<Salesman> salesmanModel;
    private JList<Salesman> salesmanList;
    private SalesReportManager reportManager;
    private AppSecurityManager securityManager;
    private UserManager userManager;
    private Map<String, LocalDateTime> lastShiftClose; // salesmanId -> last close time

    public SalesmanPanel(SalesReportManager reportManager, AppSecurityManager securityManager, UserManager userManager) {
        this.reportManager = reportManager;
        this.securityManager = securityManager;
        this.userManager = userManager;
        this.lastShiftClose = new HashMap<>();
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Salesmen Tracker"));

        salesmanModel = new DefaultListModel<>();
        salesmanList = new JList<>(salesmanModel);
        salesmanList.setFont(new Font("Monospaced", Font.PLAIN, 12));
        add(new JScrollPane(salesmanList), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton addBtn = new JButton("Add Salesman");
        addBtn.addActionListener(e -> addSalesman());
        JButton reportBtn = new JButton("Show Report");
        reportBtn.addActionListener(e -> showSalesReport());
        JButton auditBtn = new JButton("View Audit Log");
        auditBtn.addActionListener(e -> showAuditLog());
        JButton shiftBtn = new JButton("Close Shift");
        shiftBtn.addActionListener(e -> closeShift());
        JButton freeLogBtn = new JButton("Free Item Log");
        freeLogBtn.addActionListener(e -> showFreeLog());

        buttonPanel.add(addBtn);
        buttonPanel.add(reportBtn);
        buttonPanel.add(auditBtn);
        buttonPanel.add(shiftBtn);
        buttonPanel.add(freeLogBtn);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void addSalesman() {
        String name = JOptionPane.showInputDialog(this, "Enter Salesman Name:");
        if (name != null && !name.trim().isEmpty()) {
            Salesman newSalesman = new Salesman(name);
            salesmanModel.addElement(newSalesman);
        }
    }

    public void recordSaleById(String salesmanId, int amount) {
        // Find salesman by ID
        for (int i = 0; i < salesmanModel.size(); i++) {
            Salesman s = salesmanModel.get(i);
            if (s.id.equals(salesmanId)) {
                s.addSale(amount);
                salesmanList.repaint();
                return;
            }
        }
        // If not found, create a new Salesman with that ID (maybe from owner entered ID)
        Salesman newSalesman = new Salesman("Unknown", salesmanId);
        newSalesman.addSale(amount);
        salesmanModel.addElement(newSalesman);
    }

    public void sortBySales() {
        java.util.List<Salesman> list = new ArrayList<>();
        for (int i = 0; i < salesmanModel.size(); i++) list.add(salesmanModel.get(i));
        list.sort((a, b) -> Integer.compare(b.totalSales, a.totalSales));
        salesmanModel.clear();
        for (Salesman s : list) salesmanModel.addElement(s);
    }

    private void closeShift() {
        User current = userManager.getCurrentUser();
        if (current == null) return;
        String salesmanId;
        if (current.getRole().equals("SALESMAN")) {
            salesmanId = current.getSalesmanId();
        } else {
            String input = JOptionPane.showInputDialog(this, "Enter salesman ID for shift summary:");
            if (input == null || input.trim().isEmpty()) return;
            salesmanId = input.trim();
        }

        LocalDateTime since = lastShiftClose.getOrDefault(salesmanId, LocalDateTime.MIN);
        java.util.List<SaleTransaction> sales = reportManager.getSalesBySalesmanId(salesmanId);
        java.util.List<SaleTransaction> shiftSales = sales.stream()
                .filter(s -> s.getTimestamp().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().isAfter(since))
                .collect(Collectors.toList());

        int totalRevenue = shiftSales.stream().mapToInt(SaleTransaction::getFinalPrice).sum();
        long itemCount = shiftSales.size();
        long freeCount = shiftSales.stream().filter(SaleTransaction::isFree).count();

        StringBuilder summary = new StringBuilder();
        summary.append("Shift Summary for Salesman ").append(salesmanId).append("\n");
        summary.append("-----------------------------------\n");
        summary.append("Period: ").append(since == LocalDateTime.MIN ? "Beginning" : since.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .append(" to now\n");
        summary.append("Total Revenue: Rs. ").append(totalRevenue).append("\n");
        summary.append("Total Items Sold: ").append(itemCount).append("\n");
        summary.append("Free Items Given: ").append(freeCount).append("\n");

        JOptionPane.showMessageDialog(this, summary.toString(), "Shift Summary", JOptionPane.INFORMATION_MESSAGE);

        // Record shift close time
        lastShiftClose.put(salesmanId, LocalDateTime.now());
    }

    private void showFreeLog() {
        java.util.List<FreeItemLog> logs = reportManager.getFreeLogs();
        StringBuilder sb = new StringBuilder("FREE ITEM LOG\n");
        sb.append(StringUtils.repeat("=", 60)).append("\n\n");
        for (FreeItemLog log : logs) {
            sb.append(log.toString()).append("\n");
        }
        JTextArea textArea = new JTextArea(sb.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 10));
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(800, 500));
        JOptionPane.showMessageDialog(this, scrollPane, "Free Item Audit Log", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showSalesReport() {
        Map<String, Integer> salesmanTotals = reportManager.getSalesmanTotal();
        StringBuilder report = new StringBuilder("SALESMAN PERFORMANCE REPORT\n");
        report.append(StringUtils.repeat("=", 50)).append("\n\n");
        for (int i = 0; i < salesmanModel.size(); i++) {
            Salesman s = salesmanModel.get(i);
            int totalSales = salesmanTotals.getOrDefault(s.id, 0);
            int numberOfSales = reportManager.getSalesBySalesmanId(s.id).size();
            report.append(String.format("%s (ID: %s)\n", s.name, s.id));
            report.append(String.format("  Total Revenue: Rs.%,d\n", totalSales));
            report.append(String.format("  Number of Sales: %d\n", numberOfSales));
            report.append(String.format("  Average Sale: Rs.%,.2f\n",
                    numberOfSales == 0 ? 0 : totalSales / (double) numberOfSales));
            report.append("\n");
        }
        JTextArea textArea = new JTextArea(report.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 400));
        JOptionPane.showMessageDialog(this, scrollPane, "Salesman Report",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void showAuditLog() {
        java.util.List<String> auditLog = securityManager.getAuditLog();
        StringBuilder logText = new StringBuilder("AUDIT LOG\n");
        logText.append(StringUtils.repeat("=", 50)).append("\n\n");
        for (String entry : auditLog) logText.append(entry).append("\n");
        JTextArea textArea = new JTextArea(logText.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 10));
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(800, 500));
        JOptionPane.showMessageDialog(this, scrollPane, "Audit Log",
                JOptionPane.INFORMATION_MESSAGE);
    }
}

// ============================================================
// SALESMAN CLASS (now includes unique ID)
// ============================================================
class Salesman {
    String id;
    String name;
    int totalSales;

    public Salesman(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.totalSales = 0;
    }

    public Salesman(String name, String id) {
        this.name = name;
        this.id = id;
        this.totalSales = 0;
    }

    public void addSale(int amount) {
        totalSales += amount;
    }

    @Override
    public String toString() {
        return String.format("%-20s → Rs.%,6d", name, totalSales);
    }
}

// ============================================================
// INVENTORY PANEL (unchanged except file path)
// ============================================================
class InventoryPanel extends JPanel {
    private Map<String, Integer> stock;
    private DefaultListModel<String> stockModel;
    private JList<String> stockList;
    private MenuPanel menuPanel;
    private TransactionLogPanel log;

    public InventoryPanel(MenuPanel menuPanel, TransactionLogPanel log) {
        this.menuPanel = menuPanel;
        this.log = log;
        this.stock = new HashMap<>();
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Inventory Management"));
        setPreferredSize(new Dimension(0, 200));
        stockModel = new DefaultListModel<>();
        stockList = new JList<>(stockModel);
        stockList.setFont(new Font("Monospaced", Font.PLAIN, 11));
        initializeStock();
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton restockBtn = new JButton("Restock Selected");
        restockBtn.addActionListener(e -> restockItem());
        JButton bulkRestockBtn = new JButton("Bulk Restock");
        bulkRestockBtn.addActionListener(e -> bulkRestock());
        JButton lowStockBtn = new JButton("Show Low Stock");
        lowStockBtn.addActionListener(e -> showLowStock());
        buttonPanel.add(restockBtn);
        buttonPanel.add(bulkRestockBtn);
        buttonPanel.add(lowStockBtn);
        add(new JScrollPane(stockList), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        updateDisplay();
    }

    private void initializeStock() {
        stock.put("Masala Chai", 50);
        stock.put("Ginger Chai", 45);
        stock.put("Elaichi Chai", 40);
        stock.put("Cold Coffee", 30);
        stock.put("Samosa", 60);
        stock.put("Biscuits", 100);
        stock.put("Khari", 80);
        stock.put("Cake Slice", 25);
    }

    private void updateDisplay() {
        stockModel.clear();
        for (Map.Entry<String, Integer> entry : stock.entrySet()) {
            String status = entry.getValue() < 10 ? "⚠️ LOW! " :
                    (entry.getValue() < 20 ? "⚠️ " : "✓ ");
            stockModel.addElement(String.format("%s%-20s: %3d units",
                    status, entry.getKey(), entry.getValue()));
        }
    }

    private void restockItem() {
        String selected = stockList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Please select an item to restock!",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String itemName = extractItemName(selected);
        String quantityStr = JOptionPane.showInputDialog(this,
                "Enter quantity to add for " + itemName + ":", "10");
        try {
            int quantity = Integer.parseInt(quantityStr);
            if (quantity > 0) {
                stock.put(itemName, stock.getOrDefault(itemName, 0) + quantity);
                updateDisplay();
                log.addEntry("Restocked " + itemName + " with " + quantity + " units");
                JOptionPane.showMessageDialog(this,
                        "Restocked " + quantity + " units of " + itemName,
                        "Restock Complete", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid quantity!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String extractItemName(String displayString) {
        String name = displayString.substring(displayString.indexOf("✓") + 2);
        if (name.contains("LOW!")) name = name.substring(5);
        return name.substring(0, name.indexOf(":")).trim();
    }

    private void bulkRestock() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
        Map<String, JTextField> fields = new HashMap<>();
        for (String item : stock.keySet()) {
            panel.add(new JLabel(item + ":"));
            JTextField field = new JTextField("10", 5);
            fields.put(item, field);
            panel.add(field);
        }
        int result = JOptionPane.showConfirmDialog(this, panel, "Bulk Restock",
                JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            for (Map.Entry<String, JTextField> entry : fields.entrySet()) {
                try {
                    int quantity = Integer.parseInt(entry.getValue().getText());
                    if (quantity > 0) {
                        stock.put(entry.getKey(), stock.get(entry.getKey()) + quantity);
                    }
                } catch (NumberFormatException ex) { /* skip */ }
            }
            updateDisplay();
            log.addEntry("Bulk restock completed");
            JOptionPane.showMessageDialog(this, "Bulk restock completed!",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void showLowStock() {
        StringBuilder sb = new StringBuilder("LOW STOCK ITEMS (< 20 units):\n");
        sb.append(StringUtils.repeat("=", 40)).append("\n\n");
        boolean hasLowStock = false;
        for (Map.Entry<String, Integer> entry : stock.entrySet()) {
            if (entry.getValue() < 20) {
                sb.append(String.format("%-20s: %d units\n", entry.getKey(), entry.getValue()));
                hasLowStock = true;
            }
        }
        if (!hasLowStock) sb.append("All items have sufficient stock (20+ units)!");
        JTextArea textArea = new JTextArea(sb.toString());
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(400, 300));
        JOptionPane.showMessageDialog(this, scrollPane, "Low Stock Alert",
                JOptionPane.WARNING_MESSAGE);
    }

    public boolean consumeItem(String itemName) {
        if (stock.containsKey(itemName) && stock.get(itemName) > 0) {
            stock.put(itemName, stock.get(itemName) - 1);
            updateDisplay();
            return true;
        }
        return false;
    }
}

// ============================================================
// ORDER PANEL (unchanged)
// ============================================================
class OrderPanel extends JPanel {
    private DefaultListModel<String> orderModel;
    private TransactionLogPanel log;

    public OrderPanel(TransactionLogPanel log) {
        this.log = log;
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(300, 0));
        setBorder(BorderFactory.createTitledBorder("Active Orders"));
        orderModel = new DefaultListModel<>();
        JList<String> orderList = new JList<>(orderModel);
        orderList.setFont(new Font("Monospaced", Font.PLAIN, 12));
        add(new JScrollPane(orderList), BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel();
        JButton addOrderBtn = new JButton("Add Sample Order");
        addOrderBtn.addActionListener(e -> {
            String order = JOptionPane.showInputDialog(this, "Enter order details:");
            if (order != null && !order.trim().isEmpty()) addOrder(order);
        });
        JButton clearBtn = new JButton("Clear Completed Orders");
        clearBtn.addActionListener(e -> {
            orderModel.clear();
            log.addEntry("Cleared all orders.");
        });
        buttonPanel.add(addOrderBtn);
        buttonPanel.add(clearBtn);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    public void addOrder(String order) {
        orderModel.addElement(order);
        log.addEntry("New Order: " + order);
    }
}

// ============================================================
// HONESTY PANEL (now with free tea audit)
// ============================================================
class HonestyPanel extends JPanel {
    private DashboardPanel dashboard;
    private TransactionLogPanel log;
    private UserManager userManager;
    private SalesReportManager reportManager;
    private AppSecurityManager securityManager;

    public HonestyPanel(DashboardPanel dashboard, TransactionLogPanel log,
                        UserManager userManager, SalesReportManager reportManager,
                        AppSecurityManager securityManager) {
        this.dashboard = dashboard;
        this.log = log;
        this.userManager = userManager;
        this.reportManager = reportManager;
        this.securityManager = securityManager;
        setBorder(BorderFactory.createTitledBorder("Quick Honesty Actions"));
        setLayout(new FlowLayout());

        addHonestyButton("Correct Change (+10)", 10, true);
        addHonestyButton("Admitted Old Stock (+15)", 15, true);
        addHonestyButton("Fake 'Organic' Label (-20)", -20, false);
        addHonestyButton("Secret Spice Lie (-10)", -10, false);
        addHonestyButton("Refund Customer (+5)", 5, true);
        addHonestyButton("Overcharge (-15)", -15, false);
        // Free tea button (owner or salesman can award)
        JButton freeTeaBtn = new JButton("Award Free Tea");
        freeTeaBtn.addActionListener(e -> awardFreeTea());
        freeTeaBtn.setForeground(Color.BLUE);
        add(freeTeaBtn);
    }

    private void addHonestyButton(String label, int points, boolean isTruth) {
        JButton btn = new JButton(label);
        btn.setForeground(isTruth ? new Color(0, 100, 0) : Color.RED);
        btn.addActionListener(e -> {
            dashboard.adjustHonesty(points);
            log.addEntry("Honesty Action: " + label + " (" + (points > 0 ? "+" : "") + points + ")");
        });
        add(btn);
    }

    private void awardFreeTea() {
        User current = userManager.getCurrentUser();
        if (!securityManager.checkPermission(current, "SELL")) {
            JOptionPane.showMessageDialog(this,
                    "You don't have permission to award free tea!",
                    "Permission Denied", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String itemName = JOptionPane.showInputDialog(this, "Enter item name for free tea:");
        if (itemName == null || itemName.trim().isEmpty()) return;
        // Find item in menu (could be more robust)
        String salesmanId;
        if (current.getRole().equals("SALESMAN")) {
            salesmanId = current.getSalesmanId();
        } else {
            salesmanId = JOptionPane.showInputDialog(this, "Enter salesman ID:");
            if (salesmanId == null || salesmanId.trim().isEmpty()) return;
        }
        // Log free transaction (price 0)
        reportManager.recordSale(itemName, 0, 0, salesmanId, current, true);
        securityManager.logAction("Awarded free tea: " + itemName, current);
        log.addEntry("Free tea awarded: " + itemName + " to salesman " + salesmanId + " by " + current.getUsername());
        JOptionPane.showMessageDialog(this, "Free tea recorded!", "Free Item", JOptionPane.INFORMATION_MESSAGE);
    }
}

// ============================================================
// LEADERBOARD PANEL (unchanged)
// ============================================================
class LeaderboardPanel extends JPanel {
    private DefaultListModel<String> leaderboardModel;

    public LeaderboardPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Chaiwala Leaderboard"));
        leaderboardModel = new DefaultListModel<>();
        JList<String> leaderboardList = new JList<>(leaderboardModel);
        add(new JScrollPane(leaderboardList), BorderLayout.CENTER);
        JButton addBtn = new JButton("Add Chaiwala");
        addBtn.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(this, "Enter Chaiwala Name:");
            if (name != null && !name.trim().isEmpty()) {
                leaderboardModel.addElement(name + " - Starting Score: 0");
                logEntry("Added new chaiwala: " + name);
            }
        });
        add(addBtn, BorderLayout.SOUTH);
    }

    private void logEntry(String entry) {
        System.out.println(entry);
    }
}

// ============================================================
// THEME MANAGER (unchanged)
// ============================================================
class ThemeManager {
    private JFrame frame;
    private boolean isDark = false;

    public ThemeManager(JFrame frame) {
        this.frame = frame;
        JMenuBar menuBar = new JMenuBar();
        JMenu settingsMenu = new JMenu("Settings");
        JMenuItem toggleTheme = new JMenuItem("Toggle Theme");
        toggleTheme.addActionListener(e -> toggleTheme());
        settingsMenu.add(toggleTheme);
        JMenuItem happyHourItem = new JMenuItem("Set Happy Hour");
        happyHourItem.addActionListener(e -> showHappyHourDialog());
        settingsMenu.add(happyHourItem);
        menuBar.add(settingsMenu);
        frame.setJMenuBar(menuBar);
    }

    private void showHappyHourDialog() {
        HappyHourManager hh = ((Project1) frame).happyHour;
        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
        JTextField startField = new JTextField(5);
        JTextField endField = new JTextField(5);
        JTextField discountField = new JTextField(3);
        JCheckBox enableCheck = new JCheckBox("Enable Happy Hour");
        panel.add(new JLabel("Start (HH:MM):")); panel.add(startField);
        panel.add(new JLabel("End (HH:MM):")); panel.add(endField);
        panel.add(new JLabel("Discount %:")); panel.add(discountField);
        panel.add(enableCheck);
        int result = JOptionPane.showConfirmDialog(frame, panel, "Happy Hour Settings",
                JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                LocalTime start = LocalTime.parse(startField.getText());
                LocalTime end = LocalTime.parse(endField.getText());
                int discount = Integer.parseInt(discountField.getText());
                if (enableCheck.isSelected()) {
                    hh.setHappyHour(start, end, discount);
                } else {
                    hh.disable();
                }
                JOptionPane.showMessageDialog(frame, hh.getInfo());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Invalid input! Use HH:MM format and integer discount.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void applyLightTheme() {
        frame.getContentPane().setBackground(new Color(245, 245, 220));
        isDark = false;
    }

    public void applyDarkTheme() {
        frame.getContentPane().setBackground(new Color(60, 63, 65));
        isDark = true;
    }

    public void toggleTheme() {
        if (isDark) applyLightTheme();
        else applyDarkTheme();
    }
}

// ============================================================
// TRANSACTION LOG PANEL (unchanged)
// ============================================================
class TransactionLogPanel extends JPanel {
    private DefaultListModel<String> logModel;

    public TransactionLogPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Transaction Log"));
        setPreferredSize(new Dimension(0, 120));
        logModel = new DefaultListModel<>();
        JList<String> logList = new JList<>(logModel);
        logList.setFont(new Font("Monospaced", Font.PLAIN, 10));
        add(new JScrollPane(logList), BorderLayout.CENTER);
    }

    public void addEntry(String entry) {
        String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
        logModel.addElement("[" + timestamp + "] " + entry);
    }
}

// ============================================================
// PERSISTENCE MANAGER (now uses data folder)
// ============================================================
class PersistenceManager {
    private File saveFile = new File(Project1.DATA_DIR + "/chaiLiarData.txt");

    public void saveData(int sales, int honesty) {
        try (PrintWriter pw = new PrintWriter(saveFile)) {
            pw.println(sales);
            pw.println(honesty);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void loadData(DashboardPanel dashboard) {
        if (!saveFile.exists()) return;
        try (Scanner sc = new Scanner(saveFile)) {
            int sales = sc.nextInt();
            int honesty = sc.nextInt();
            dashboard.addSale(sales);
            dashboard.adjustHonesty(honesty - dashboard.getHonestyScore());
        } catch (Exception e) { e.printStackTrace(); }
    }
}

// ============================================================
// FEEDBACK SYSTEM (unchanged)
// ============================================================
class FeedbackSystem {
    private javax.swing.Timer timer;

    public void attachToDashboard(DashboardPanel dashboard, TransactionLogPanel log) {
        timer = new javax.swing.Timer(20000, e -> {
            Random rand = new Random();
            int mood = rand.nextInt(100);
            if (mood < 40) {
                dashboard.adjustHonesty(-5);
                log.addEntry("👎 Customer complained: Tea too sweet!");
            } else if (mood > 70) {
                dashboard.adjustHonesty(+5);
                log.addEntry("👍 Customer praised: Best chai ever!");
            } else {
                log.addEntry("😐 Customer was neutral.");
            }
        });
        timer.start();
    }

    public void stopFeedback() {
        if (timer != null) timer.stop();
    }
}
