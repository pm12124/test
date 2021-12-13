package ext.deere.creoviewmbdtest.view;

import com.ptc.pview.pvkapp.*;
import com.ptc.pview.utils.dom.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import java.io.File;
import java.util.*;
import java.lang.Exception;

/**
 * This example was built starting from the BuildAssemblyExample and ViewStateExample of the Creo View Java Toolkit
 * examples.
 */
public class MBDExample extends ExamplesBase implements Printable {
    String partNumber = "";
    String partVersion = "";
    String partRev = "";
    
    //public BufferedImage image;

    ArrayList<ViewStateSource> listOfViewStates;
    
    String printJobType = "";
    
    LinkedHashMap<String, BOMTableItem> bomTable;



    JMenuItem printModel = null;
    JMenuItem printAllViews = null;
    JMenuItem viewBOM = null;

    JMenu printMenu;
    JMenu bomMenu;

    MyActor myActor;
    
    public MBDExample(String title) {
        super(title);
        setSize(1440, 900);
        setLocationRelativeTo(null);
        init();
    }
    
    public static void main(String[] args) {
        MBDExample app = new MBDExample("Creo View Java Toolkit : MBD Viewer");
        app.runPView();
    }

    synchronized public boolean runPView() {

        if (pview.IsPviewInstalled() == false)
            return false;

        if (pview.Start("webserver") == false)
            return false;

        try {
            myActor = new MyActor();
            kernel = myActor.getKernel();
            theWindow = kernel.GetWindow();
            pvWindow = new PVWindow(theWindow, panel);
            // Get a handle to the embedded control to initialise the application.
            embeddedControl = kernel.GetEmbeddedControl();
            // Create a single instance of the World
            theWorld = embeddedControl.CreateWorld();

            theWorld.SetParentWindow(pvWindow.GetWindow());
            /**
             * Setting the control actor to "pview" determines Creo View is
             * used with full UI depending on licensing.
             */
            //MCAD Version
            //theWorld.SetControlActor("pvpro");

            //Lite Version
            //theWorld.SetControlActor("pvlite");

            //theWorld.SetControlActor("pview");

            theWorld.SetControlActor("thumbnail");

            embeddedControl.SetAutoLoad("auto");
            System.out.println(embeddedControl.GetTempDir());

            //embeddedControl.SetBackgroundColor(240, 240);

            myActor.listenForEvents();

            AsyncEventCB initEvent = myActor.getAsyncEvent("Initialise");
            embeddedControl.Initialise(initEvent.GetAsyncEventIf());

            addWindowListener( new WindowAdapter() {
                /**
                 * Catch when the user closes the application so Creo View is
                 * shutdown correctly.
                 */
                public void windowClosing(WindowEvent e) {
                    pviewShutdown();
                    System.exit(0);
                }
            });
        } catch (Throwable x) {
            return false;
        }
        return true;
    }

    /**
     * Called after Creo View is started and opens the hardcoded file.
     */
    void pviewInitialised() {
        try {

            AsyncEventCB urlOpenEvent = myActor.getAsyncEvent("URLOpen");
            myActor.ManageObject(urlOpenEvent.GetAsyncEventIf());
//            embeddedControl.URLOpen("c:/temp/at2938.pvz", "", "",
            embeddedControl.URLOpen("c:/temp/at2905.pvz", "", "",
                    urlOpenEvent.GetAsyncEventIf());
//            embeddedControl.URLOpen("https://agwcsb6.tal.deere.com/Windchill/servlet/WindchillAuthGW/com.ptc.wvs.server.util.WVSContentHelper/redirectDownload/at2938_asm.pvs?ContentHolder=wt.viewmarkup.MultiFidelityDerivedImage%3A51849557582&HttpOperationItem=wt.content.ApplicationData%3A51849557625&u8=1&objref=OR%3Awt.viewmarkup.MultiFidelityDerivedImage%3A51849557582&elink=true", "", "",
//                    urlOpenEvent.GetAsyncEventIf());
        } catch (Throwable x) {
        }
    }

    /**
     * Current menu bar menu button listener implementation, performs the main custom functionality
     * @param e
     */
    public void actionPerformed(ActionEvent e) {
        System.out.println("in actionPerformed" + e);
        // Get root part/assembly information like part number, rev, etc to be used in the print output.
        getRootData();
        if (e.getSource() == printModel) {
            AsyncEventCB saveImageEvent = myActor.getAsyncEvent("ViewImageSaved");
            try {
                // Save the current view shape scene to a image file to print.  After the image is saved the async
                // event will call the print dialog to print.
                ShapeScene scene = theWorld.GetFirstShapeScene();
                ShapeView view = scene.GetShapeView(0);
                view.SaveImage(embeddedControl.GetTempDir() + "vs-cur.bmp",
                        1600, 1200, 1000, saveImageEvent.GetAsyncEventIf());
                printJobType = "printCurrent";
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else if (e.getSource() == printAllViews) {
            // This will go through the list of view states and load them and save an image of them and then call the 
            // print dialog using the following method calls:
            
            // Load the bom table information by visiting all of the structure and load the view states of the root 
            // element while that is being visited.  Doing this here, because couldn't find a way to do to be told when
            // a file is loaded.  Would want to limit this to just being called once per file instead of right now, I
            // have it being called on both BOM menu and Print all views menu.
            visitStructureToGetData();
            
            printJobType = "printAll";
            if (listOfViewStates.size() > 0)
                openViewStateForPrint(0);
            else 
                JOptionPane.showMessageDialog(this, "There are no combined state views to print.");
        } else if (e.getSource() == viewBOM) {
            // Display the BOM Table.
            
            visitStructureToGetData();
            
            displayBOMTable();
        }
    }

    /**
     * Load up the main gui menu items
     * @return
     */
    synchronized public boolean init() {
        try {
            JMenuBar  menuBar = new JMenuBar();
            printMenu = new JMenu();

            printModel = new JMenuItem();
            printModel.setText( " Print Current " );
            printModel.addActionListener(this);

            printAllViews = new JMenuItem();
            printAllViews.setText( " Print All Views " );
            printAllViews.addActionListener(this);
            
            printMenu.getPopupMenu().setLightWeightPopupEnabled(false);
            printMenu.setText("Print");
            printMenu.add(printModel);
            printMenu.add(printAllViews);
            menuBar.add(printMenu);

            bomMenu = new JMenu();

            viewBOM = new JMenuItem();
            viewBOM.setText( " View Flat BOM " );
            viewBOM.addActionListener(this);

            bomMenu.getPopupMenu().setLightWeightPopupEnabled(false);
            bomMenu.setText("BOM");
            bomMenu.add(viewBOM);
            menuBar.add(bomMenu);
            
            setJMenuBar(menuBar);

            validate();
        } catch (Throwable x) {
            return false;
        }
        return true;
    }

    /**
     * Get root part/assembly information like part number, rev, etc to be used in the print output.
     */
    private void getRootData() {
        try {
            Instance root = theWorld.GetTree().GetRoot();
            partNumber = root.GetProperty("PROE Parameters", "PARTNUMBER");
            partVersion = root.GetProperty("PROE Parameters", "VERSION");
            partRev = root.GetProperty("PROE Parameters", "REVISIONLEVEL");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     *  Load the bom table information by visiting all of the structure and load the view states of the root 
     *  element while that is being visited.  Doing this here, because couldn't find a way to do to be told when
     *  a file is loaded.  Would want to limit this to just being called once per file instead of right now, I
     *  have it being called on both BOM menu and Print all views menu.
     */
    private void visitStructureToGetData() {
        listOfViewStates = new ArrayList<ViewStateSource>();
        bomTable = new LinkedHashMap<String, BOMTableItem>();
        try {
            InstanceVisitorEventHandler ive = new InstanceVisitorEventHandler();
            InstanceVisitorImpl iv = new InstanceVisitorImpl();
            iv.SetEventHandler(ive);
            myActor.ManageObject(iv);
            
            // Previous example of traversing the structure, not used anymore, using the visit logic, since we have to
            // use that to get the view states.
            //getStructure(theWorld.GetTree().GetRoot(), 0);

            theWorld.GetTree().GetRoot().Visit(iv, 0);
        } catch (Throwable x) {
        }
    }

    /**
     *  Previous example of traversing the structure, not used anymore, using the visit logic, since we have to
     *  use that to get the view states.  Is recursively called to load the bom table from the structure
     * @param parent
     * @param level
     */
    private void getStructure(Instance parent, int level) {
        try {
            BOMTableItem bomItem = bomTable.get(parent.GetName() + "," + level);
            if (bomItem == null) {
                bomTable.put(parent.GetName() + "," + level, new BOMTableItem(parent.GetProperty("PROE Parameters", "PARTNUMBER"), parent.GetProperty("PROE Parameters", "REVISIONLEVEL"), parent.GetProperty("PROE Parameters", "PARTNAME"), parent.GetProperty("PROE Parameters", "PARTDESCRIPTION")));
            } else {
                bomItem.incrementQty();
            }
            System.out.println(parent.GetName());
            Instance firstChild = parent.GetFirstChild();
            Instance nextSibling = parent.GetNextSibling();
            if (firstChild != null) {
                getStructure(firstChild, level+1);
            }
    
            if (nextSibling != null) {
                getStructure(nextSibling, level);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
    }

    /**
     * Builds a JTable from our BOM Table data structure
     * @return
     */
    private JTable getBOMTable() {
        Vector bomTableVector = new Vector();
        int row = 1;
        for (Map.Entry<String, BOMTableItem> bomItem:bomTable.entrySet()) {
            BOMTableItem currentItem = bomItem.getValue();
            Vector bomItemData = new Vector();
            bomItemData.add(row);
            bomItemData.add(currentItem.getNumber());
            bomItemData.add(currentItem.getRev());
            bomItemData.add(currentItem.getName());
            bomItemData.add(currentItem.getDescription());
            bomItemData.add(currentItem.getQty());
            bomTableVector.add(bomItemData);
            row++;
        }

        return new JTable(bomTableVector, new Vector(Arrays.asList("Item", "Number", "Rev", "Name", "Description", "Qty")));        
    }

    /**
     *  Displays a JTable for the BOM Table in a Modal Dialog
      */

    private void displayBOMTable() {
        JDialog bomDialog = new JDialog(this, "BOM", true);
        bomDialog.setSize(700, 500);
        JTable bomTable = getBOMTable();
        JScrollPane scrollPane = new JScrollPane(bomTable);
        bomDialog.add(scrollPane);
        bomDialog.setLocationRelativeTo(null);
        bomDialog.setVisible(true);
    }

    /**
     * This method is called after all the views are saved as images to open the page dialog and eventually the print
     * dialog.  It also handles combining the pages of the views printables with the bom printable.  Lots of things were
     * tried for this and can be cleaned up.
     */
    private void openPrintDialog() {
        PrinterJob job = PrinterJob.getPrinterJob();
        // Open Page Dialog for formatting of the page, we could enhance this and set defaults for margin, orientation,
        // etc.
        PageFormat pf = job.pageDialog(job.defaultPage());
        if (printJobType == "printCurrent")
            job.setPrintable(this);
        else {
            // Using a book to combine the views printable with the BOM Table printable.  Easiest way to print a table
            // of data at this time was to print a JTable.  Might be a better solution, but this was easiest for a PoC.
            Book book = new Book();
            
            // Had to put the BOM Table into a JFrame to be able to have it be visible in the print, otherwise it just 
            // showed a black box outline unless it was on a frame.
            JFrame bomFrame = new JFrame();
            JTable bomTable = getBOMTable();
            JScrollPane scrollPane = new JScrollPane(bomTable);
            bomFrame.add(scrollPane);
            bomFrame.pack();

            Printable bomPrintable = bomTable.getPrintable(JTable.PrintMode.FIT_WIDTH, null, null);

            // The BOM table would only work correctly with a book if it was the first page, seemed to get overwritten if
            // it was the last page, an example solution to this issue that I implemented below was found at 
            // https://stackoverflow.com/questions/14775753/printing-multiple-jtables-as-one-job-book-object-only-prints-1st-table
            int totalPages = 0;
            book.append(new PrintableWrapper(this, totalPages), pf, listOfViewStates.size());
            totalPages = listOfViewStates.size();

            int pages = 0;
            try {
                pages = getNumberOfPages(bomPrintable, pf);
            } catch (PrinterException e) {
                e.printStackTrace();
            }
            book.append (new PrintableWrapper(bomPrintable, totalPages), pf, pages);
            totalPages += pages;

            // This didn't work if the BOM table was the last page
//            book.append(this, pf, listOfViewStates.size());
//            book.append(bomPrintable, pf, 1);

            job.setPageable(book);
        }
        
        // Open the Print Dialog
        boolean ok = job.printDialog();
        if (ok) {
            try {
                job.print();
            } catch (PrinterException ex) {
                /* The job did not successfully complete */
            }
        }
    }

    public int getNumberOfPages(Printable delegate, PageFormat pageFormat) throws PrinterException
    {
        Graphics g = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB).createGraphics();
        int numPages = 0;
        while (true) {
            int result = delegate.print(g, pageFormat, numPages);
            if (result == Printable.PAGE_EXISTS) {
                ++numPages;
            } else {
                break;
            }
        }

        return numPages;
    }

    @Override
    /**
     * Main method that is called to build the printable pages for the combined state views that were saved off as
     * images in previous logic.  This builds each page and puts the border frame and example text in the right hand
     * corner, adds the image from the file for the view and also adds the watermark in the top left hand corner.
     * This method has lots of room for improvement and was just done quickly to show what possbilities are and prove
     * out that we could do most of what is needed.
     */
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {

        // This method would be called never ending with endless pages, so it has to have some logic to determine that
        // there are no more pages to print and return the NO_SUCH_PAGE when that is true.
        if((printJobType.equals("printAll") && pageIndex >= listOfViewStates.size()) || (printJobType.equals("printCurrent") && pageIndex > 0)) {
            return NO_SUCH_PAGE;
        }
        
        // Translate the x and y axis to be 0,0 for the area that is a printable region, otherwise it would start x,y 
        // with the values of where the margins would put it.
        Graphics2D g2d = (Graphics2D)graphics;
        g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

        // Get the actual printable area on the page in height and width to be used to fit future page content
        int printWidth = (int) Math.round(pageFormat.getImageableWidth());
        int printHeight = (int) Math.round(pageFormat.getImageableHeight());
        
        // Print the Model information, this is the model attribute values in the top right hand corner.
        graphics.drawString(partNumber, printWidth-190, 25);
        graphics.drawString(partVersion, printWidth-110, 25);
        int totalPages = 1;
        
        if (printJobType.equals( "printAll"))
            totalPages = listOfViewStates.size();
        
        graphics.drawString((pageIndex + 1) + "/" + totalPages, printWidth-70, 25);
        graphics.drawString(partRev, printWidth-37, 25);

        // Print the View name in the bottom right corner
        if (printJobType.equals( "printAll")) {
            try {
                graphics.drawString("VIEW = " + listOfViewStates.get(pageIndex).GetName(), printWidth-175, printHeight-10);
            } catch (Exception ex) {
                
            }
        }
        
        // Draw page outlines
        graphics.drawRect(0, 0, printWidth, printHeight);
        graphics.drawRect(20, 30, printWidth-40, printHeight-60);
        
        // Draw top data boxes
        graphics.drawRect(printWidth-40, 2, 20, 28);
        graphics.drawRect(printWidth-73, 2, 33, 28);
        graphics.drawRect(printWidth-113, 2, 40, 28);
        graphics.drawRect(printWidth-193, 2, 80, 28);

        // Add the labels to the top data boxes
        graphics.setFont(new Font("TimesRoman", Font.PLAIN, 6));
        graphics.drawString("PART NUMBER", printWidth-190, 10);
        graphics.drawString("VERSION", printWidth-110, 10);
        graphics.drawString("SHEET", printWidth-70, 10);
        graphics.drawString("REV", printWidth-37, 10);

        // Add the saved image of the view from the file system to the printout based on the page
        try {
            BufferedImage image = null;
            if (printJobType.equals("printCurrent")) 
                image = ImageIO.read(new File(embeddedControl.GetTempDir() + "vs-cur.bmp"));
            else 
                image = ImageIO.read(new File(embeddedControl.GetTempDir() + "vs-" + pageIndex + ".bmp"));
            
            // Size the image to fit within the printable area and within our painted border.  This should size based
            // on the page size, margin, etc the user choose.
            Dimension newSize = getScaledDimension(new Dimension(image.getWidth(), image.getHeight()), new Dimension(printWidth-46, printHeight-66));
            graphics.drawImage(image, printWidth- newSize.width-23, printHeight- newSize.height-33, newSize.width, newSize.height, null);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // Draw Watermark
        graphics.setFont(new Font(null, Font.PLAIN, 12));
        graphics.setColor(new Color(225, 225, 225));
        drawRotate(g2d, 27, 190, -40,"JDS-G113.4 CODE 5 DERIVATIVE");
        drawRotate(g2d, 27, 205, -40,"FOR REFERENCE ONLY, MODEL IS MASTER");
        
        /* tell the caller that this page is part of the printed document */
        return PAGE_EXISTS;
    }

    public static void drawRotate(Graphics2D g2d, double x, double y, int angle, String text)
    {
        g2d.translate((float)x,(float)y);
        g2d.rotate(Math.toRadians(angle));
        g2d.drawString(text,0,0);
        g2d.rotate(-Math.toRadians(angle));
        g2d.translate(-(float)x,-(float)y);
    }

    // Based on the passed in image size and boundry size, it returns the best size to use to fit in the boundry keeping
    // the aspect ratio.  Taken from https://stackoverflow.com/questions/10245220/java-image-resize-maintain-aspect-ratio
    public static Dimension getScaledDimension(Dimension imgSize, Dimension boundary) {

        int original_width = imgSize.width;
        int original_height = imgSize.height;
        int bound_width = boundary.width;
        int bound_height = boundary.height;
        int new_width = original_width;
        int new_height = original_height;

        // first check if we need to scale width
        if (original_width > bound_width) {
            //scale width to fit
            new_width = bound_width;
            //scale height to maintain aspect ratio
            new_height = (new_width * original_height) / original_width;
        }

        // then check if we need to scale even with the new height
        if (new_height > bound_height) {
            //scale height to fit instead
            new_height = bound_height;
            //scale width to maintain aspect ratio
            new_width = (new_height * original_width) / original_height;
        }

        return new Dimension(new_width, new_height);
    }

    /**
     * This method will set the View state based on the passed in index so that an image of it can be captured for
     * printing.
     * @param viewStateIndex
     */
    private void openViewStateForPrint(int viewStateIndex) {
        AsyncEventCB viewStateAsyncEv = new MBDExampleAsyncEvent(
                "setViewStateForPrint:" + viewStateIndex);
        myActor.ManageObject(viewStateAsyncEv
                .GetAsyncEventIf());

        try {
            theWorld.GetFirstShapeScene()
                    .SetViewState(listOfViewStates.get(viewStateIndex), theWorld.GetTree().GetRoot(),
                            viewStateAsyncEv.GetAsyncEventIf());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * This method will save the current view to a file to be used for print and save it with the index in the file name
     * so it can be retrieved for the correct view.
     * @param viewIndex
     */
    public void zoomView(int viewIndex) {
        try {
            AsyncEventCB zoomViewEvent = myActor.getAsyncEvent("setZoomForPrint:" + viewIndex);
            ShapeScene scene = theWorld.GetFirstShapeScene();
            ShapeView view = scene.GetShapeView(0);
            view.ZoomAll(zoomViewEvent.GetAsyncEventIf());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    /**
     * This method will save the current view to a file to be used for print and save it with the index in the file name
     * so it can be retrieved for the correct view.
     * @param viewIndex
     */
    public void saveViewToImage(int viewIndex) {
        try {
            AsyncEventCB saveImageEvent = myActor.getAsyncEvent("ViewImageSaved:" + viewIndex);
            ShapeScene scene = theWorld.GetFirstShapeScene();
            ShapeView view = scene.GetShapeView(0);
            view.SaveImage(embeddedControl.GetTempDir() + "vs-" + viewIndex + ".bmp",
                    1600, 1200, 2400, saveImageEvent.GetAsyncEventIf());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    class MyActor extends ManagedObject {
        public MyActor() {
            super();
            queue = new MessageQueue();
            ManageSelf(queue, "Actor");
        }
        /**
         *  This class is required for Creo View message processing.
         */
        class CheckForMessages implements ActionListener{

            public void actionPerformed(ActionEvent event) {

                try {

                    if (queue.IsInService()) {


                        Message m = queue.PollMessage();
                        if (m != null) {

                            System.out.println ("Message found: "+ m.toString());
                            queue.ForwardMessageToHandler(m);
                        }
                    } else {
                        queue.ForwardMessageToHandler(null);
                    }
                } catch(InvalidActorException e) {
                    msgTimer.stop();
                } catch(ConnectionLostException e) {
                    msgTimer.stop();
                } catch(MessageProtocolException e) {
                    msgTimer.stop();
                } catch(ActorShutdownException e) {
                    msgTimer.stop();
                }
            }
        };

        public MenuItemEvent getMenuItemEvent(){

            MenuItemEvent ev = new MyMenuItemEvent();
            ManageObject (ev.GetMenuItemIf());
            return ev;
        }

        public AsyncEventCB getAsyncEvent(String use) {
            AsyncEventCB as = new MBDExampleAsyncEvent(use);
            ManageObject (as.GetAsyncEventIf());
            return as;
        }

        public Kernel getKernel() {
            try {
                return (Kernel)GetDistinguishedObject(Kernel.CLASS_NAME,
                        "pvkernel");
            } catch (Throwable x) {
            }
            return null;
        }
        
        public void listenForEvents() {
            System.out.println("listenForEvents() adding timer to check for messages\n");
            CheckForMessages cfm = new CheckForMessages();
            msgTimer = new Timer(10,cfm);
            msgTimer.start();
        }

        public String GetObjectClass() {
            return "pvexamples::pvexamplesutilities::MyActor";
        }

        private MessageQueue queue;
        private Timer msgTimer;
    };

    public class MBDExampleAsyncEvent extends AsyncEventCB{
        private String m_description;

        MBDExampleAsyncEvent(String reason) {
            m_description =reason;
        }

        public void OnProgress(long progress) {
        }

        public void OnComplete(long status) {
            try {
                if (m_description == "Initialise") {
                    pviewInitialised();
                } else if (m_description == "URLOpen") {
                    //fileOpened();
                } else if (m_description == "ViewImageSaved") {
                    // Called for printing just the current view, will be called on the completion of the current view
                    // being saved.  Then call the print dialog logic to complete the print option.
                    openPrintDialog();
                } else if (m_description.startsWith("setViewStateForPrint:")) {
                    // This will be called after an image is saved and the next view has been displayed so that we save
                    // this next view as an image.

                    // Had to add the sleep, as it appears this is called before the full animation finishes.  When a
                    // view is loaded is does that animation to get to it's final position and this seems to be called
                    // before that is finished.
                    Thread.sleep(1000);

                    // Based on the passed in index, go save the image for that view based on this index.
                    zoomView(Integer.parseInt(m_description.split(":")[1]));
                    //saveViewToImage(Integer.parseInt(m_description.split(":")[1]));
                } else if (m_description.startsWith("setZoomForPrint:")) {
                    // This will be called after an image is saved and the next view has been displayed so that we save
                    // this next view as an image.

                    // Had to add the sleep, as it appears this is called before the full animation finishes.  When a
                    // view is loaded is does that animation to get to it's final position and this seems to be called
                    // before that is finished.
                    Thread.sleep(1000);

                    // Based on the passed in index, go save the image for that view based on this index.
                    saveViewToImage(Integer.parseInt(m_description.split(":")[1]));
                }  else if (m_description.startsWith("ViewImageSaved:")) {
                    // This will be called after the view is set the image is saved to call the open view for the next
                    // view state based on the index passed in or to open the print dialog if it is the last view.
                    int prevViewIndex = Integer.parseInt(m_description.split(":")[1]);
                    int curViewIndex = prevViewIndex + 1;
                    if (curViewIndex < listOfViewStates.size()) {
                        openViewStateForPrint(curViewIndex);
                    } else {
                        openPrintDialog();
                    }
                }
            } catch (Exception ex) {
            }
        }
    }

    class InstanceVisitorEventHandler extends InstanceVisitorEvents {
        public InstanceVisitorEventHandler() {
            super();
        }

        /**
         * This method will be called for each item in the structure
         * 
         * @param inst
         * @param parent
         * @param depth
         * @return
         */
        public boolean Visit(Instance inst, Instance parent, int depth) {
            
            try {
                // Build the bom table as we visit each node in the structure.  If the item already exists in the BOM
                // table update it's quantity, if not add it.
                BOMTableItem bomItem = bomTable.get(inst.GetName());
                if (bomItem == null) {
                    bomTable.put(inst.GetName(), new BOMTableItem(inst.GetProperty("PROE Parameters", "PARTNUMBER"), inst.GetProperty("PROE Parameters", "REVISIONLEVEL"), inst.GetProperty("PROE Parameters", "PARTNAME"), inst.GetProperty("PROE Parameters", "PARTDESCRIPTION1")));
                } else {
                    bomItem.incrementQty();
                }
                
                // Only for the root element get the combined state views
                if (depth == 0) {
                    ViewStateVisitorEventHandler vsve = new ViewStateVisitorEventHandler(inst);
                    ViewStateVisitorImpl vsvimpl = new ViewStateVisitorImpl();
                    vsvimpl.SetEventHandler(vsve);
                    myActor.ManageObject(vsvimpl);
                    inst.GetComponentNode().Visit(vsvimpl);
                }
            } catch (Throwable x) {
                return false;
            }
            return true;
        }
    };

    class ViewStateVisitorEventHandler extends ViewStateVisitorEvents {
        public ViewStateVisitorEventHandler(Instance inst) {
            super();
            instance = inst;
        }

        /**
         * Add the View State to our list of combined state views for the root model.
         * @param viewStateSource
         * @return
         */
        public boolean Visit(
                com.ptc.pview.pvkapp.ViewStateSource viewStateSource) {
            try {
                // Believe the VIEW_STATE type are the only ones we care about at this point
                if (viewStateSource.GetType().equals(ViewStateSourceType.VIEW_STATE))
                    listOfViewStates.add(viewStateSource);
            } catch (Throwable x) {
                return false;
            }
            return true;
        }

        private Instance instance;
    };
    

    class PrintableWrapper implements Printable
    {
        private Printable delegate;
        private int offset;

        public PrintableWrapper(Printable delegate, int offset) {
            this.offset = offset;
            this.delegate = delegate;
        }

        @Override
        public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
            return delegate.print(graphics, pageFormat, pageIndex-offset);
        }
    };
}
