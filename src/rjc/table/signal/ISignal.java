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

public interface ISignal
{
  static class SignalHelper
  {
    final private static WeakHashMap<ISignal, ArrayList<IListener>> m_listeners      = new WeakHashMap<>();
    final private static WeakHashMap<ISignal, ArrayList<IListener>> m_laterListeners = new WeakHashMap<>();

    /****************************************** signal *******************************************/
    private static void signal( ISignal signaller, Object[] objects )
    {
      // send signal objects to each listener registered with specified signal sender
      var list = m_listeners.get( signaller );
      if ( list != null )
        list.forEach( ( listener ) -> listener.slot( signaller, objects ) );
      list = m_laterListeners.get( signaller );
      if ( list != null )
        list.forEach( ( listener ) -> Platform.runLater( () -> listener.slot( signaller, objects ) ) );
    }

    /**************************************** signalLater ****************************************/
    private static void signalLater( ISignal signaller, Object[] objects )
    {
      // send signal objects using Platform.runLater to each listener registered with specified signal sender
      Platform.runLater( () -> signal( signaller, objects ) );
    }

    /*************************************** addListener *****************************************/
    private static void addListener( ISignal signaller, IListener listener )
    {
      // register listener for specified signal sender
      var list = m_listeners.get( signaller );
      if ( list == null )
      {
        list = new ArrayList<>();
        m_listeners.put( signaller, list );
      }
      list.add( listener );
    }

    /************************************* addLaterListener **************************************/
    private static void addLaterListener( ISignal signaller, IListener listener )
    {
      // register listener for specified signal sender
      var list = m_laterListeners.get( signaller );
      if ( list == null )
      {
        list = new ArrayList<>();
        m_laterListeners.put( signaller, list );
      }
      list.add( listener );
    }

    /************************************** removeListener ***************************************/
    private static void removeListener( ISignal signaller, IListener listener )
    {
      // unregister listener for specified signal sender, no-action if not present
      var list = m_listeners.get( signaller );
      if ( list != null )
        list.remove( listener );
      list = m_laterListeners.get( signaller );
      if ( list != null )
        list.remove( listener );
    }

    /************************************ removeAllListeners *************************************/
    private static void removeAllListeners( ISignal signaller )
    {
      // unregister all listeners for specified signal sender
      var list = m_listeners.get( signaller );
      if ( list != null )
        list.clear();
      list = m_laterListeners.get( signaller );
      if ( list != null )
        list.clear();
    }

    /*************************************** getListeners ****************************************/
    private static ArrayList<IListener> getListeners( ISignal signaller )
    {
      // return list of all listeners for specified signal sender, can be null
      return m_listeners.get( signaller );
    }

    /************************************* getLaterListeners *************************************/
    private static ArrayList<IListener> getLaterListeners( ISignal signaller )
    {
      // return list of all listeners for specified signal sender, can be null
      return m_laterListeners.get( signaller );
    }
  }

  /******************************************* signal ********************************************/
  default void signal( Object... objects )
  {
    // default implementation for sending immediate signal to listeners
    SignalHelper.signal( this, objects );
  }

  /***************************************** signalLater *****************************************/
  default void signalLater( Object... objects )
  {
    // default implementation for sending delayed signal to listeners
    SignalHelper.signalLater( this, objects );
  }

  /***************************************** addListener *****************************************/
  default void addListener( IListener listener )
  {
    // default implementation for adding a listener to a signal sender
    SignalHelper.addListener( this, listener );
  }

  /************************************** addLaterListener ***************************************/
  default void addLaterListener( IListener listener )
  {
    // default implementation for adding a later listener to a signal sender
    SignalHelper.addLaterListener( this, listener );
  }

  /*************************************** removeListener ****************************************/
  default void removeListener( IListener listener )
  {
    // default implementation for removing a listener from a signal sender (later & not-later)
    SignalHelper.removeListener( this, listener );
  }

  /************************************* removeAllListeners **************************************/
  default void removeAllListeners()
  {
    // default implementation for removing all listeners from a signal sender (later & not-later)
    SignalHelper.removeAllListeners( this );
  }

  /**************************************** getListeners *****************************************/
  default ArrayList<IListener> getListeners()
  {
    // default implementation for getting list of all listeners for a signal sender (does not include later)
    return SignalHelper.getListeners( this );
  }

  /************************************** getLaterListeners **************************************/
  default ArrayList<IListener> getLaterListeners()
  {
    // default implementation for getting list of all listeners for a signal sender (does not include not-later)
    return SignalHelper.getLaterListeners( this );
  }

}