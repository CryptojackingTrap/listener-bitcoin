package it.unitn;

import com.google.bitcoin.core.*;
import com.google.bitcoin.net.discovery.DnsDiscovery;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.store.BlockStore;
import com.google.bitcoin.store.BlockStoreException;
import com.google.bitcoin.store.MemoryBlockStore;
import com.google.bitcoin.utils.BriefLogFormatter;
import com.google.bitcoin.utils.Threading;
import com.google.common.collect.Lists;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * @author Atefeh Zareh
 * <p>
 * Shows connected peers in a table view, so you can watch as they come
 * and go. you can change maximum number of peers that you can use to
 * understand the latest block from Bitcoin network. Note that this
 * class is written upon PeerMonitor.java and BuildCheckpoints.java in
 * BitcoinJ projects
 */
public class BlockChainListener {

    private static final String IMG_PATH = "./src/main/resources/logo.png";

    private NetworkParameters params;
    private BlockChain chain;
    private PeerGroup peerGroup;
    private PeerTableModel peerTableModel;
    private PeerTableRenderer peerTableRenderer;

    private final HashMap<Peer, String> reverseDnsLookups = new HashMap<Peer, String>();


    public static void main(String[] args) throws Exception {
        BriefLogFormatter.init();
        new BlockChainListener();

    }

    public BlockChainListener() throws UnknownHostException,
            BlockStoreException {
        setupNetwork();
        setupGUI();
    }

    private void setupNetwork() throws BlockStoreException,
            UnknownHostException {
        /*
         * Logger is inactive. to see new transactions active it.
         */
        // BriefLogFormatter.init();
        params = MainNetParams.get();

        // Sorted map of UNIX time of block to StoredBlock object.
        // final TreeMap<Integer, StoredBlock> checkpoints = new
        // TreeMap<Integer, StoredBlock>();

        // Configure bitcoinj to fetch only headers, not save them to disk,
        // connect to a local fully synced/validated
        // node and to save block headers that are on interval boundaries, as
        // long as they are <1 month old.
        final BlockStore store = new MemoryBlockStore(params);
        chain = new BlockChain(params, store);
        peerGroup = new PeerGroup(params, chain);

        peerGroup.addAddress(InetAddress.getLocalHost());
        long now = new Date().getTime() / 1000;
        peerGroup.setFastCatchupTimeSecs(now);

        // Zareh added
        peerGroup.setMaxConnections(4);
        peerGroup.addPeerDiscovery(new DnsDiscovery(params));
        //

        peerGroup.setUserAgent("BlockChainListener", "1.0");

        peerGroup.addEventListener(new AbstractPeerEventListener() {
            @Override
            public void onPeerConnected(final Peer peer, int peerCount) {
                refreshUI();
                lookupReverseDNS(peer);
            }

            @Override
            public void onPeerDisconnected(final Peer peer, int peerCount) {
                refreshUI();
                synchronized (reverseDnsLookups) {
                    reverseDnsLookups.remove(peer);
                }
            }
        });
    }

    private static Integer blockNumber = 0;

    private void setupBlockChain(final Date startDateToLog) throws BlockStoreException,
            UnknownHostException {

        chain.addListener(new AbstractBlockChainListener() {
            @Override
            public void notifyNewBestBlock(StoredBlock block)
                    throws VerificationException {
                /**
                 * it start to download blocks from first block.
                 */
                int height = block.getHeight();
                Sha256Hash prevHash = block.getHeader().getPrevBlockHash();
                Sha256Hash itsHash = block.getHeader().getHash();

                // if (height % params.getInterval() == 0 || freshBlock(block))
                // {
                Date blockDate = block.getHeader().getTime();
                System.out.println(String.format(
                        "%d - %s - Checkpointing block %s at height %d and prevHash=%s",
                        blockNumber, blockDate.toString(), itsHash, height, prevHash));
                blockNumber++;
                /*
                 * log the block hash to file, if blockDate is newer (larger)
                 * than startDateToLog
                 */
                if ((blockDate.compareTo(startDateToLog)) > 0) {
                    String hexStr = HexUtils.bytesToHex(itsHash.getBytes());
                    hexStr = hexStr.toLowerCase();
                    try {
                        writeHash(hexStr, new Date(), block.getHeader().getTime());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            // private boolean freshBlock(StoredBlock block) {
            // long now = new Date().getTime() / 1000;
            // long fiveMinutesAgo = now - (5 * 60);
            // return (block.getHeader().getTimeSeconds() >= fiveMinutesAgo);
            // }
        }, Threading.SAME_THREAD);
    }

    //------------------------------- logging into file -----------------------
    private static final String DATE_FORMAT_PATTERN = "(yyyy/MM/dd, HH:mm:ss)";
    private final static String outFileRelativePath = "src/test/resources/latestBlockHash-Bitcoin.txt";
    static FileChannel channel;

    static {
        File file = new File(outFileRelativePath);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(outFileRelativePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        channel = fout.getChannel();
    }

    private static void writeHash(String latestHash, Date receiveDate,
                                  Date blockDate) throws IOException {
        String genaralDateFormat = DATE_FORMAT_PATTERN;// hh for 12 format
        // clock and HH for
        // 24
        DateFormat dateFormat = new SimpleDateFormat(genaralDateFormat);
        String receiveDateStr = dateFormat.format(receiveDate);
        String blockDateStr = dateFormat.format(blockDate);
        // Integer lineSize = SHA_OUT_LENGTH_IN_HEX+
        // genaralDateFormat.length()*2 + 2;

        // buf.clear();
        String line = latestHash + " " + receiveDateStr + " " + blockDateStr
                + "\n";
        ByteBuffer buf = ByteBuffer.allocate(line.length());
        buf.put(line.getBytes());
        buf.flip();
        while (buf.hasRemaining()) {
            channel.write(buf);
        }
    }
    //----------------------------- end of logging into file ----------------

    private void lookupReverseDNS(final Peer peer) {
        new Thread() {
            @Override
            public void run() {
                // This can take a looooong time.
                String reverseDns = peer.getAddress().getAddr()
                        .getCanonicalHostName();
                synchronized (reverseDnsLookups) {
                    reverseDnsLookups.put(peer, reverseDns);
                }
                refreshUI();
            }
        }.start();
    }

    private void refreshUI() {
        // Tell the Swing UI thread to redraw the peers table.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                peerTableModel.updateFromPeerGroup();
            }
        });
    }

    private void setupGUI() {
        JFrame window = new JFrame("Cryptojackingtrap - Bitcoin Network Listener");
        window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                System.out.println("Shutting down ...");
                peerGroup.stopAndWait();
                System.out.println("Shutdown complete.");
                System.exit(0);
            }
        });

        JPanel panel = new JPanel();
        JLabel instructions = new JLabel("Number of peers to connect to: ");
        final SpinnerNumberModel spinnerModel = new SpinnerNumberModel(4, 0,
                100, 1);
        spinnerModel.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent changeEvent) {
                peerGroup
                        .setMaxConnections(spinnerModel.getNumber().intValue());
            }
        });
        JSpinner numPeersSpinner = new JSpinner(spinnerModel);
        panel.add(instructions);
        panel.add(numPeersSpinner);
        // window.getContentPane().add(panel, BorderLayout.NORTH);

        peerTableModel = new PeerTableModel();
        JTable peerTable = new JTable(peerTableModel);
        peerTable.setAutoCreateRowSorter(true);
        peerTableRenderer = new PeerTableRenderer(peerTableModel);
        peerTable.setDefaultRenderer(String.class, peerTableRenderer);
        peerTable.setDefaultRenderer(Integer.class, peerTableRenderer);
        peerTable.setDefaultRenderer(Long.class, peerTableRenderer);
        peerTable.getColumnModel().getColumn(0).setPreferredWidth(300);

        JScrollPane scrollPane = new JScrollPane(peerTable);
        // window.getContentPane().add(scrollPane, BorderLayout.CENTER);

        // final JTextArea area = new JTextArea("A");
        // JButton startBtn = new JButton("Start");
        // startBtn.addActionListener(new ActionListener() {
        // public void actionPerformed(ActionEvent e) {
        // area.setText("B");
        // }
        // });
        //
        // window.getContentPane().add(area, BorderLayout.EAST);
        // window.getContentPane().add(startBtn, BorderLayout.WEST);
        Timer timer = new Timer(1000, new ActionListener() {

            public void actionPerformed(ActionEvent actionEvent) {
                peerTableModel.updateFromPeerGroup();
            }
        });
        // zareh
        /*
         * JTextArea jTextArea = new JTextArea(
         * "program is starting to get all Bitcoin block chain... ");
         * jTextArea.setSize(720, 240); jTextArea.setAutoscrolls(true);
         * jTextArea.setEditable(true); JScrollPane textScrollPane = new
         * JScrollPane(jTextArea); textScrollPane.setSize(720, 240);
         * window.getContentPane().add(textScrollPane, BorderLayout.SOUTH);
         */

        // logo picture
        BufferedImage img = null;
        try {
            img = ImageIO.read(new File(IMG_PATH));
        } catch (IOException e) {
            e.printStackTrace();
        }
        ImageIcon logoImage = new ImageIcon(img);
        JLabel logoLable = new JLabel(logoImage);

        // Log
        JTextArea jtxtLog = new JTextArea(
                "program is starting to get all Bitcoin block chain... ");
        jtxtLog.setSize(720, 240);
        jtxtLog.setEditable(false);
        jtxtLog.setAutoscrolls(true);
        jtxtLog.setEditable(true);

        JScrollPane jtxtLogScrollPane = new JScrollPane(jtxtLog);
        jtxtLogScrollPane.setSize(720, 240);

        // choose data and time
        JSpinner timeSpinner = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor timeEditor = new JSpinner.DateEditor(timeSpinner,
                "yyyy/MM/dd  HH:mm:ss");
        timeSpinner.setEditor(timeEditor);
        timeSpinner.setValue(new Date()); // will only show the current time

        JPanel jpDate = new JPanel();
        JButton jButton = new JButton("Start");

        jpDate.add(timeSpinner);
        jpDate.add(jButton);

        StartBlockListener startBlockListener = new StartBlockListener(jButton,
                timer, timeSpinner, this);
        jButton.addActionListener(startBlockListener);

        Box box = new Box(BoxLayout.Y_AXIS);
        box.add(logoLable);
        box.add(panel);
        box.add(scrollPane);
        box.add(jpDate);
        box.add(jtxtLogScrollPane);

        window.getContentPane().add(box, BorderLayout.NORTH);

        window.pack();
        window.setSize(720, 680);
        window.setVisible(true);
    }

    private class PeerTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 4105059721804566228L;
        public static final int IP_ADDRESS = 0;
        public static final int PROTOCOL_VERSION = 1;
        public static final int USER_AGENT = 2;
        public static final int CHAIN_HEIGHT = 3;
        public static final int PING_TIME = 4;
        public static final int LAST_PING_TIME = 5;

        public List<Peer> connectedPeers = Lists.newArrayList();
        public List<Peer> pendingPeers = Lists.newArrayList();

        public void updateFromPeerGroup() {
            connectedPeers = peerGroup.getConnectedPeers();
            pendingPeers = peerGroup.getPendingPeers();
            fireTableDataChanged();
        }

        public int getRowCount() {
            return connectedPeers.size() + pendingPeers.size();
        }

        @Override
        public String getColumnName(int i) {
            switch (i) {
                case IP_ADDRESS:
                    return "Address";
                case PROTOCOL_VERSION:
                    return "Protocol version";
                case USER_AGENT:
                    return "User Agent";
                case CHAIN_HEIGHT:
                    return "Chain height";
                case PING_TIME:
                    return "Average ping";
                case LAST_PING_TIME:
                    return "Last ping";
                default:
                    throw new RuntimeException();
            }
        }

        public int getColumnCount() {
            return 6;
        }

        public Class<?> getColumnClass(int column) {
            switch (column) {
                case PROTOCOL_VERSION:
                    return Integer.class;
                case CHAIN_HEIGHT:
                case PING_TIME:
                case LAST_PING_TIME:
                    return Long.class;
                default:
                    return String.class;
            }
        }

        public Object getValueAt(int row, int col) {
            if (row >= connectedPeers.size()) {
                // Peer that isn't connected yet.
                Peer peer = pendingPeers.get(row - connectedPeers.size());
                switch (col) {
                    case IP_ADDRESS:
                        return getAddressForPeer(peer);
                    case PROTOCOL_VERSION:
                        return 0;
                    case CHAIN_HEIGHT:
                    case PING_TIME:
                    case LAST_PING_TIME:
                        return 0L;
                    default:
                        return "(pending)";
                }
            }
            Peer peer = connectedPeers.get(row);
            switch (col) {
                case IP_ADDRESS:
                    return getAddressForPeer(peer);
                case PROTOCOL_VERSION:
                    return Integer
                            .toString(peer.getPeerVersionMessage().clientVersion);
                case USER_AGENT:
                    return peer.getPeerVersionMessage().subVer;
                case CHAIN_HEIGHT:
                    return peer.getBestHeight();
                case PING_TIME:
                case LAST_PING_TIME:
                    return col == PING_TIME ? peer.getPingTime() : peer
                            .getLastPingTime();

                default:
                    throw new RuntimeException();
            }
        }

        private Object getAddressForPeer(Peer peer) {
            String s;
            synchronized (reverseDnsLookups) {
                s = reverseDnsLookups.get(peer);
            }
            if (s != null)
                return s;
            else
                return peer.getAddress().getAddr().getHostAddress();
        }
    }

    private class PeerTableRenderer extends JLabel implements TableCellRenderer {
        private static final long serialVersionUID = -8335139265782789701L;
        private final PeerTableModel model;
        private final Font normal, bold;

        public PeerTableRenderer(PeerTableModel model) {
            super();
            this.model = model;
            this.normal = new Font("Sans Serif", Font.PLAIN, 12);
            this.bold = new Font("Sans Serif", Font.BOLD, 12);
        }

        public Component getTableCellRendererComponent(JTable table,
                                                       Object contents, boolean selected, boolean hasFocus, int row,
                                                       int column) {
            row = table.convertRowIndexToModel(row);
            column = table.convertColumnIndexToModel(column);

            String str = contents.toString();
            if (model.connectedPeers == null || model.pendingPeers == null) {
                setText(str);
                return this;
            }

            if (row >= model.connectedPeers.size()) {
                setFont(normal);
                setForeground(Color.LIGHT_GRAY);
            } else {
                if (model.connectedPeers.get(row) == peerGroup
                        .getDownloadPeer())
                    setFont(bold);
                else
                    setFont(normal);
                setForeground(Color.BLACK);

                // Mark chain heights that aren't normal but not for pending
                // peers, as we don't know their heights yet.
                if (column == PeerTableModel.CHAIN_HEIGHT) {
                    long height = (Long) contents;
                    if (height != peerGroup.getMostCommonChainHeight()) {
                        str = height + " \u2022 ";
                    }
                }
            }

            boolean isPingColumn = column == PeerTableModel.PING_TIME
                    || column == PeerTableModel.LAST_PING_TIME;
            if (isPingColumn && contents.equals(Long.MAX_VALUE)) {
                // We don't know the answer yet
                str = "";
            }
            setText(str);
            return this;
        }

    }

    class StartBlockListener implements ActionListener {
        private JButton jButton;
        private Timer timer;
        private JSpinner jSpinner;
        private BlockChainListener blockChainListener;

        public StartBlockListener(JButton jButton, Timer timer,
                                  JSpinner jSpinner, BlockChainListener blockChainListener) {
            this.jButton = jButton;
            this.timer = timer;
            this.jSpinner = jSpinner;
            this.blockChainListener = blockChainListener;
        }

        public void actionPerformed(ActionEvent e) {
            Date startDateToLog = (Date) jSpinner.getValue();
            timer.start();
            jButton.setEnabled(false);
            jSpinner.setEnabled(false);

            try {
                blockChainListener.setupBlockChain(startDateToLog);
            } catch (UnknownHostException e1) {
                e1.printStackTrace();
            } catch (BlockStoreException e1) {
                e1.printStackTrace();
            }

            peerGroup.startAndWait();
            peerGroup.downloadBlockChain();

        }
    }
}
