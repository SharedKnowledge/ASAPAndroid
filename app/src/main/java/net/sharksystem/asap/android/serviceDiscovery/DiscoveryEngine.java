package net.sharksystem.asap.android.serviceDiscovery;


import android.util.Log;

import java.util.ArrayList;

public abstract class DiscoveryEngine
{
    /**
     * Classname for logging
     */
    private final String TAG = this.getClass().getSimpleName();

    protected ArrayList<ServiceDescription> servicesToLookFor = new ArrayList<>();

    protected boolean engineRunning = false;

    protected boolean engineIsNotRunning(){
        return !engineRunning;
    }

    /**
     * Returns true if the engine was started successfully
     * This needs a working BluetoothAdapter to be available on the device
     *
     * @return
     * running state of the engine
     */
    public boolean isRunning(){
        return engineRunning;
    }


    //
    //  ----------  add service for discovery ----------
    //

    public void startDiscoveryForService(ServiceDescription description){
        if(engineIsNotRunning()){
            Log.e(TAG, "startSDPDiscoveryForService: engine is not running - wont start");
        }
        Log.d(TAG, "Starting service discovery");
        // Are we already looking for he service?
        if (this.isServiceAlreadyInDiscovery(description))
        {
            Log.d(TAG, "startSDPDiscoveryForServiceWithUUID: Service discovery already running ");
            return;
        }
        // Adding the service to  be found in the future
        this.servicesToLookFor.add(description);

        // subclasses call
        onNewServiceToDiscover(description);
    }

    /**
     * called whenever a new service was added to the discovery
     * can be implemented in subclasses
     * @param description
     * the description of the service
     */
    protected abstract void onNewServiceToDiscover(ServiceDescription description);


    //
    //  ----------  remove service from discovery ----------
    //

    public void stopDiscoveryForService(ServiceDescription description)
    {
        if(engineIsNotRunning()){
            Log.e(TAG, "stopSDPDiscoveryForService: engine is not running - wont start");
        }
        Log.d(TAG, "End service discovery for service with UUID " + description.toString());
        // removing from list of services
        this.servicesToLookFor.remove(description);

        // subclasses call
        onServiceRemoveFromDiscovery(description);
    }

    /**
     * called whenever a  service was removed to the discovery
     * can be implemented in subclasses
     * @param description
     * the description of the service
     */
    protected abstract void onServiceRemoveFromDiscovery(ServiceDescription description);


    /**
     * Checks if the service description is already in {@link #servicesToLookFor} list
     *
     * @param description
     *         Description of the service to look for
     *
     * @return false if the service is not in the list, else returns true
     */
    protected boolean isServiceAlreadyInDiscovery(ServiceDescription description)
    {
        return servicesToLookFor.contains(description);
    }

}
