package ext.deere.test;

import com.ptc.pview.pvkapp.*;
import com.ptc.pview.utils.dom.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

/**
 * This example was built starting from the BuildAssemblyExample and ViewStateExample of the Creo View Java Toolkit
 * examples.
 */
public class ExampleProperty extends ExamplesBase {

    FileWriter writer = null;
    MyActor myActor;
    
    public ExampleProperty(String title) {
        super(title);
        setSize(1440, 900);
        setLocationRelativeTo(null);
    }
    
    public static void main(String[] args) {
        ExampleProperty app = new ExampleProperty("Creo View Java Toolkit : MBD Viewer");
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
            embeddedControl.URLOpen("c:/temp/etn158329.pvz", "", "",
                urlOpenEvent.GetAsyncEventIf());
        } catch (Throwable x) {
        }
    }

    private void visitStructureToGetData() {
        try {
            InstanceVisitorEventHandler ive = new InstanceVisitorEventHandler();
            InstanceVisitorImpl iv = new InstanceVisitorImpl();
            iv.SetEventHandler(ive);
            myActor.ManageObject(iv);


            theWorld.GetTree().GetRoot().Visit(iv, 0);
            ive.visitProperties();
        } catch (Throwable x) {
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

    public class MBDExampleAsyncEvent extends AsyncEventCB {
        private String m_description;

        MBDExampleAsyncEvent(String reason) {
            m_description = reason;
        }

        public void OnProgress(long progress) {
        }

        public void OnComplete(long status) {
            

            if (m_description == "Initialise") {
                pviewInitialised();
            } else if (m_description == "URLOpen") {
                try {
                    writer = new FileWriter("C15950775.log");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                visitStructureToGetData();

                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } 
        }
    }

    class InstanceVisitorEventHandler extends InstanceVisitorEvents {
        Vector<Instance > vec = new Vector<Instance >();

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
                vec.add(inst);
            } catch (Throwable x) {
                return false;
            }
            return true;
        }

        public void visitProperties() {
			Iterator<Instance> itr = vec.iterator();  
            int count = 0;
            while(itr.hasNext()){  
                try {
                    Instance inst = itr.next();
                    writer.write("--------------" + inst.GetName() + "---------------\n");
                    writer.write("--------------" + (++count) + "---------------\n");
                    writer.flush();

                    PropertyVisitEvent propertyVisitEvent = new PropertyVisitEvent();
                    PropertiesVisitorImpl propertiesVisitorImpl = new PropertiesVisitorImpl();
                    propertiesVisitorImpl.SetEventHandler(propertyVisitEvent);
                    myActor.ManageObject(propertiesVisitorImpl);

                	inst.Visit(propertiesVisitorImpl);

                    writer.write("-------------Data Loaded-------------\n");
                    writer.flush();
                } catch (Throwable x) {
                    x.printStackTrace();
			        return;
    			} 
            }   
		}
    };

   
    class PropertyVisitEvent extends PropertiesVisitorEvents {
        public boolean Visit(Property property) {
//            System.out.println("hello");
            try {
                try {
                    writer.write(property.GetName() + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // System.out.println(property.GetName());
            } catch (MessageProtocolException e) {
                e.printStackTrace();
            } catch (ActorShutdownException e) {
                e.printStackTrace();
            } catch (InvalidActorException e) {
                e.printStackTrace();
            } catch (ConnectionLostException e) {
                e.printStackTrace();
            }
            return true;
        }        
    }
}
