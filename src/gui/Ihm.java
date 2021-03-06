/*
 * Creation : 17 févr. 2018
 */
package gui;

import java.awt.Container;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.TransferHandler;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableModel;

import aif.Aif;
import de.rechner.openatfx_mdf.mdf3.Mdf3Util;
import utils.Utilitaire;

public final class Ihm extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final String INFO = "<html><b>INFO : Outil pour rechercher des fichiers de donn&eacute;es contenant certaines variables</b><br>"
            + "<br><u>Proc&eacute;dure :</u></br>" + "<ul><li>S&eacute;lectionner le dossier contenant les acquisitions (fichiers dat ou aif)"
            + "<li>Renseigner les variables (en respectant la casse, ex : \"Ext_nEng\", \"TECSMOT1\"...) &agrave; rechercher en les s&eacute;parant par un point virgule ou glisser directement un fichier lab"
            + "<li>Lancer la recherche, les r&eacute;sultats apparaitront dans le tableau ci-dessous</ul>" + "</html>";

    private static Path pathFolder;
    private static List<String> signalToSearch;
    private static final String[] COLUMN = { "Fichier(s)" };
    private static int nbFileFounded = 0;

    final JTextField field;
    final JLabel selectedFolder;
    final JLabel nbResult;
    private static DefaultTableModel model;

    private static final GridBagConstraints gbc = new GridBagConstraints();

    public Ihm() {
        super("DataFileSearch");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        final Container root = getContentPane();

        root.setLayout(new GridBagLayout());

        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(5, 5, 0, 5);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        root.add(new JLabel(INFO), gbc);

        final JButton btFolder = new JButton("S\u00e9lectionner r\u00e9pertoire");
        btFolder.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser();
                fc.setMultiSelectionEnabled(false);
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int reponse = fc.showOpenDialog(Ihm.this);

                if (reponse == JFileChooser.APPROVE_OPTION) {
                    pathFolder = fc.getSelectedFile().toPath();
                    selectedFolder.setText("Dossier choisi : " + pathFolder);
                    nbResult.setText("Nombre de r\u00e9sultat :");
                    model.setRowCount(0);
                }
            }
        });
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(5, 5, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        root.add(btFolder, gbc);

        selectedFolder = new JLabel("Dossier choisi : ");
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.insets = new Insets(5, 5, 0, 0);
        gbc.anchor = GridBagConstraints.WEST;
        root.add(selectedFolder, gbc);

        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(5, 5, 0, 0);
        gbc.anchor = GridBagConstraints.CENTER;
        root.add(new JLabel("Variable(s) \u00e0 rechercher :"), gbc);

        field = new JTextField(60);
        field.setToolTipText("Ex : Ext_nEng;AccP_rAccp;Veh_spdVeh");
        field.setTransferHandler(new TransfertLab());

        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.insets = new Insets(5, 0, 0, 5);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        root.add(field, gbc);

        final JButton btCheckSignal = new JButton("Lancer la recherche");
        btCheckSignal.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                model.setRowCount(0);
                nbResult.setText("Nombre de r\u00e9sultat :");
                final String filterName = field.getText();

                if (filterName.length() > 0) {

                    signalToSearch = new ArrayList<String>();

                    File fileFilter = new File(filterName);
                    if (fileFilter.exists()) {
                        parseFileFilter(fileFilter);
                    } else {
                        for (String s : filterName.split(";")) {
                            if (s.trim().length() > 0) {
                                signalToSearch.add(s.trim());
                            }
                        }
                    }

                    new Thread(new Runnable() {

                        @Override
                        public void run() {
                            final Cursor oldCursor = Cursor.getDefaultCursor();
                            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                            searchSignalName();
                            nbResult.setText("Nombre de r\u00e9sultat : " + model.getRowCount() + "/" + nbFileFounded);
                            setCursor(oldCursor);
                        }
                    }).start();

                }
            }
        });
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(5, 5, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        root.add(btCheckSignal, gbc);

        nbResult = new JLabel("Nombre de r\u00e9sultat :");
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(5, 5, 0, 0);
        gbc.anchor = GridBagConstraints.CENTER;
        root.add(nbResult, gbc);

        model = new DefaultTableModel(new String[0][0], COLUMN) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        final JTable table = new JTable(model);
        table.setToolTipText("Double click pour ouvrir le dossier");
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 1;
        gbc.insets = new Insets(0, 5, 0, 5);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        root.add(new JScrollPane(table), gbc);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && e.getSource() == table) {
                    final String seletedPath = (String) model.getValueAt(table.getSelectedRow(), table.getSelectedColumn());

                    final File directory = new File(seletedPath).getParentFile();
                    Desktop desktop = null;

                    if (Desktop.isDesktopSupported()) {
                        desktop = Desktop.getDesktop();
                        try {
                            desktop.open(directory);
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                }
            }
        });

        final JButton btSave = new JButton("Enregistrer r\u00e9sultats");
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(5, 5, 5, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        root.add(btSave, gbc);

        btSave.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                if (model.getRowCount() > 0) {
                    final JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setDialogTitle("Enregistement des r\u00e9sultats");
                    fileChooser.setFileFilter(new FileFilter() {

                        @Override
                        public String getDescription() {
                            return "Fichier texte (*.txt)";
                        }

                        @Override
                        public boolean accept(File f) {
                            return Utilitaire.getExtension(f).equals("txt");
                        }
                    });

                    final Date date = new Date();
                    final SimpleDateFormat formater = new SimpleDateFormat("yyMMdd");

                    fileChooser.setSelectedFile(new File(formater.format(date) + "_MDFTools_Resultats.txt"));
                    final int rep = fileChooser.showSaveDialog(null);

                    if (rep == JFileChooser.APPROVE_OPTION) {
                        final File rapport;

                        if (fileChooser.getSelectedFile().getName().endsWith(".txt")) {
                            rapport = fileChooser.getSelectedFile();
                        } else {
                            rapport = new File(fileChooser.getSelectedFile().getAbsolutePath() + ".txt");
                        }

                        PrintWriter printWriter = null;

                        try {
                            printWriter = new PrintWriter(rapport);

                            printWriter.println(" ----------- ");
                            printWriter.println("| RESULTATS |");
                            printWriter.println(" ----------- ");

                            printWriter.println("\nDossier analyse : " + pathFolder);

                            printWriter.println("\nVariable(s) recherchees : " + field.getText().trim());

                            printWriter.println("\nFichier(s) trouve(s) :");
                            for (int idx = 0; idx < model.getRowCount(); idx++) {
                                printWriter.println(model.getValueAt(idx, 0));
                            }

                            JOptionPane.showMessageDialog(null, "Enregistrement termine !\n" + rapport, null, JOptionPane.INFORMATION_MESSAGE);

                        } catch (FileNotFoundException e1) {
                            e1.printStackTrace();
                        } finally {
                            if (printWriter != null) {
                                printWriter.close();
                            }
                        }
                    }
                }
            }
        });

        pack();
        setMinimumSize(new Dimension(getWidth(), getHeight()));
    }

    private final void parseFileFilter(File file) {

        try (BufferedReader buf = new BufferedReader(new InputStreamReader(new FileInputStream(file), Charset.forName("ISO-8859-1")))) {
            String line;

            String extension = Utilitaire.getExtension(file);

            switch (extension) {
            case "lab":
                while ((line = buf.readLine()) != null) {
                    if (!line.equals("[RAMCELL]")) {
                        signalToSearch.add(line.trim());
                    }
                }
                break;
            case "cec":

                boolean pass = false;

                while ((line = buf.readLine()) != null) {

                    if (!pass && line.startsWith("[Mesures=")) {
                        pass = true;
                        line = buf.readLine();
                    }

                    if (pass) {
                        signalToSearch.add(line.trim());
                    }
                }
                break;
            default:
                break;
            }

        } catch (

        IOException e) {
            e.printStackTrace();
        }
    }

    private static void searchSignalName() {
        if (pathFolder != null) {
            try {
                final Finder finder = new Finder();
                Files.walkFileTree(pathFolder, finder);

                for (int i = 0; i < finder.listMdfFileName.size(); i++) {
                    model.addRow(new Object[] { finder.listMdfFileName.get(i) });
                }

                if (finder.listSignalName != null) {
                    finder.listSignalName.clear();
                }

                signalToSearch.clear();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static class Finder extends SimpleFileVisitor<Path> {

        Set<String> listSignalName;
        final List<String> listMdfFileName = new ArrayList<String>();

        public Finder() {
            nbFileFounded = 0;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

            String extension = Utilitaire.getExtension(file.toFile());

            switch (extension) {
            case "dat":
                listSignalName = Mdf3Util.getListSignalName(file);
                break;
            case "aif":
                listSignalName = Aif.getDatasetList(file.toFile());
                break;
            default:
                listSignalName = Collections.emptySet();
                break;
            }

            if (!listSignalName.isEmpty()) {
                if (listSignalName.containsAll(signalToSearch)) {
                    listMdfFileName.add(file.toString());
                }
                nbFileFounded++;
            }

            return FileVisitResult.CONTINUE;
        }
    }

    private final class TransfertLab extends TransferHandler {

        private static final long serialVersionUID = 1L;

        @Override
        public boolean canImport(TransferSupport info) {

            if (!info.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                return false;
            }
            return true;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean importData(TransferSupport info) {

            if (!info.isDrop()) {
                return false;
            }

            final Transferable objetTransfer = info.getTransferable();

            List<File> dropFiles;
            try {
                dropFiles = (List<File>) objetTransfer.getTransferData(DataFlavor.javaFileListFlavor);
                field.setText(dropFiles.get(0).getAbsolutePath());

            } catch (UnsupportedFlavorException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return true;
        }
    }
}
