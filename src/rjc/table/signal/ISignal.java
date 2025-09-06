/**************************************************************************
 *  Copyright (C) 2025 by Richard Crook                                   *
 *  https://github.com/dazzle50/JTableFX                                  *
 *                                                                        *
 *  This program is free software: you can redistribute it and/or modify  *
 *  it under the terms of the GNU General Public License as published by  *
 *  the Free Software Foundation, either version 3 of the License, or     *
 *  (at your option) any later version.                                   *
 *                                                                        *
 *  This program is distributed in the hope that it will be useful,       *
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *  GNU General Public License for more details.                          *
 *                                                                        *
 *  You should have received a copy of the GNU General Public License     *
 *  along with this program.  If not, see http://www.gnu.org/licenses/    *
 **************************************************************************/

package rjc.table.signal;

import java.util.ArrayList;
import java.util.WeakHashMap;

import javafx.application.Platform;

/*************************************************************************************************/
/******************* Interface for signal senders with default implementation ********************/
/*************************************************************************************************/

/**
 * Interface providing signal/slot functionality with default implementations.
 * Classes implementing this interface can emit signals to registered listeners.
 * Supports both immediate and JavaFX Platform.runLater() delayed signal delivery.
 * Uses weak references to prevent memory leaks when signal senders are garbage collected.
 */
public interface ISignal
{
  /**
   * Helper class that manages signal listeners and provides the core signal/slot functionality.
   * Uses WeakHashMap to automatically clean up listeners when signal senders are garbage collected.
   * Maintains separate collections for immediate and delayed (Platform.runLater) listeners.
   */
  final static class SignalHelper
  {
    final private static WeakHashMap<ISignal, ArrayList<IListener>> m_listeners      = new WeakHashMap<>();
    final private static WeakHashMap<ISignal, ArrayList<IListener>> m_laterListeners = new WeakHashMap<>();

    /****************************************** signal *******************************************/
    /**
     * Sends immediate signal to all registered listeners for the specified sender.
     * Also sends signal to laterListeners using JavaFX Platform.runLater().
     * 
     * @param signaller the signal sender
     * @param objects message parameters to send to listeners
     */
    final private static void signal( ISignal signaller, Object[] objects )
    {
      // send signal objects to each immediate listener registered with specified signal sender
      var list = m_listeners.get( signaller );
      if ( list != null )
        list.forEach( ( listener ) -> listener.slot( signaller, objects ) );
      // also send to later listeners via JavaFX Platform.runLater
      list = m_laterListeners.get( signaller );
      if ( list != null )
        list.forEach( ( listener ) -> Platform.runLater( () -> listener.slot( signaller, objects ) ) );
    }

    /**************************************** signalLater ****************************************/
    /**
     * Sends delayed signal using Platform.runLater to all registered listeners.
     * 
     * @param signaller the signal sender
     * @param objects message parameters to send to listeners
     */
    final private static void signalLater( ISignal signaller, Object[] objects )
    {
      // defer signal delivery to JavaFX application thread using Platform.runLater
      Platform.runLater( () -> signal( signaller, objects ) );
    }

    /*************************************** addListener *****************************************/
    /**
     * Registers an immediate listener for the specified signal sender.
     * 
     * @param signaller the signal sender to listen to
     * @param listener the listener to register
     */
    final private static void addListener( ISignal signaller, IListener listener )
    {
      // get or create listener list for this signal sender
      var list = m_listeners.get( signaller );
      if ( list == null )
      {
        list = new ArrayList<>();
        m_listeners.put( signaller, list );
      }
      // add listener to the list
      list.add( listener );
    }

    /************************************* addLaterListener **************************************/
    /**
     * Registers a delayed listener that receives signals via Platform.runLater.
     * 
     * @param signaller the signal sender to listen to
     * @param listener the listener to register for delayed delivery
     */
    final private static void addLaterListener( ISignal signaller, IListener listener )
    {
      // get or create later listener list for this signal sender
      var list = m_laterListeners.get( signaller );
      if ( list == null )
      {
        list = new ArrayList<>();
        m_laterListeners.put( signaller, list );
      }
      // add listener to the later list
      list.add( listener );
    }

    /************************************** removeListener ***************************************/
    /**
     * Unregisters a listener from both immediate and delayed listener collections.
     * Safe to call even if listener is not registered - no action taken.
     * 
     * @param signaller the signal sender to remove listener from
     * @param listener the listener to unregister
     */
    final private static void removeListener( ISignal signaller, IListener listener )
    {
      // remove from immediate listeners if present
      var list = m_listeners.get( signaller );
      if ( list != null )
        list.remove( listener );
      // remove from later listeners if present
      list = m_laterListeners.get( signaller );
      if ( list != null )
        list.remove( listener );
    }

    /************************************ removeAllListeners *************************************/
    /**
     * Unregisters all listeners (both immediate and delayed) for the specified sender.
     * Clears both listener collections but does not remove the sender from the maps.
     * 
     * @param signaller the signal sender to remove all listeners from
     */
    final private static void removeAllListeners( ISignal signaller )
    {
      // clear immediate listeners list
      var list = m_listeners.get( signaller );
      if ( list != null )
        list.clear();
      // clear later listeners list
      list = m_laterListeners.get( signaller );
      if ( list != null )
        list.clear();
    }

    /*************************************** getListeners ****************************************/
    /**
     * Returns the list of immediate listeners for the specified sender.
     * Does not include delayed listeners - use getLaterListeners() for those.
     * 
     * @param signaller the signal sender to get listeners for
     * @return list of immediate listeners, or null if no listeners registered
     */
    final private static ArrayList<IListener> getListeners( ISignal signaller )
    {
      // return immediate listeners list (can be null if no listeners)
      return m_listeners.get( signaller );
    }

    /************************************* getLaterListeners *************************************/
    /**
     * Returns the list of delayed listeners for the specified sender.
     * Does not include immediate listeners - use getListeners() for those.
     * 
     * @param signaller the signal sender to get later listeners for
     * @return list of delayed listeners, or null if no later listeners registered
     */
    final private static ArrayList<IListener> getLaterListeners( ISignal signaller )
    {
      // return later listeners list (can be null if no later listeners)
      return m_laterListeners.get( signaller );
    }
  }

  /******************************************* signal ********************************************/
  /**
   * Sends an immediate signal to all registered listeners with optional message parameters.
   * Listeners are called directly on the current thread, followed by delayed listeners
   * which are called via Platform.runLater().
   * 
   * @param objects variable number of message objects to send to listeners
   */
  default void signal( Object... objects )
  {
    // delegate to helper for immediate signal delivery
    SignalHelper.signal( this, objects );
  }

  /***************************************** signalLater *****************************************/
  /**
   * Sends a delayed signal to all registered listeners using Platform.runLater().
   * Signal delivery is deferred to the JavaFX Application Thread event queue.
   * Useful for ensuring UI updates happen on the correct thread.
   * 
   * @param objects variable number of message objects to send to listeners
   */
  default void signalLater( Object... objects )
  {
    // delegate to helper for delayed signal delivery
    SignalHelper.signalLater( this, objects );
  }

  /***************************************** addListener *****************************************/
  /**
   * Registers a listener to receive immediate signals from this sender.
   * The listener will be called directly on the thread that emits the signal.
   * 
   * @param listener the listener to register for immediate signal delivery
   */
  default void addListener( IListener listener )
  {
    // delegate to helper for adding immediate listener
    SignalHelper.addListener( this, listener );
  }

  /************************************** addLaterListener ***************************************/
  /**
   * Registers a listener to receive delayed signals via Platform.runLater().
   * The listener will be called on the JavaFX Application Thread regardless
   * of which thread emits the signal.
   * 
   * @param listener the listener to register for delayed signal delivery
   */
  default void addLaterListener( IListener listener )
  {
    // delegate to helper for adding delayed listener
    SignalHelper.addLaterListener( this, listener );
  }

  /*************************************** removeListener ****************************************/
  /**
   * Unregisters a listener from both immediate and delayed signal delivery.
   * Safe to call even if the listener was never registered.
   * 
   * @param listener the listener to unregister from all signal types
   */
  default void removeListener( IListener listener )
  {
    // delegate to helper for removing listener from both collections
    SignalHelper.removeListener( this, listener );
  }

  /************************************* removeAllListeners **************************************/
  /**
   * Unregisters all listeners (both immediate and delayed) from this signal sender.
   * Clears all listener collections but preserves the sender in the weak hash maps.
   */
  default void removeAllListeners()
  {
    // delegate to helper for removing all listeners
    SignalHelper.removeAllListeners( this );
  }

  /**************************************** getListeners *****************************************/
  /**
   * Returns the list of immediate listeners registered with this signal sender.
   * Does not include delayed listeners - use getLaterListeners() for those.
   * 
   * @return list of immediate listeners, or null if no immediate listeners are registered
   */
  default ArrayList<IListener> getListeners()
  {
    // delegate to helper for getting immediate listeners
    return SignalHelper.getListeners( this );
  }

  /************************************** getLaterListeners **************************************/
  /**
   * Returns the list of delayed listeners registered with this signal sender.
   * Does not include immediate listeners - use getListeners() for those.
   * 
   * @return list of delayed listeners, or null if no delayed listeners are registered
   */
  default ArrayList<IListener> getLaterListeners()
  {
    // delegate to helper for getting delayed listeners
    return SignalHelper.getLaterListeners( this );
  }

}