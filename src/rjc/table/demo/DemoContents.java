/**************************************************************************
 *  Copyright (C) 2024 by Richard Crook                                   *
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

package rjc.table.demo;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import rjc.table.Utils;
import rjc.table.signal.ObservableStatus;
import rjc.table.signal.ObservableStatus.Level;
import rjc.table.undo.UndoStack;
import rjc.table.undo.UndoStackWindow;

/*************************************************************************************************/
/****************************** Contents of demo application window ******************************/
/*************************************************************************************************/

public class DemoContents extends GridPane
{
  private ObservableStatus m_status;     // status associated with status bar

  private UndoStack        m_undostack;  // shared undostack for whole demo application
  private UndoStackWindow  m_undoWindow; // window to interact with undo-stack

  /**************************************** constructor ******************************************/
  public DemoContents()
  {
    // create demo window layout
    add( getMenuBar(), 0, 0 );

    var tabs = getTabs();
    add( tabs, 0, 1 );
    setHgrow( tabs, Priority.ALWAYS );
    setVgrow( tabs, Priority.ALWAYS );

    add( getStatusBar(), 0, 2 );
  }

  /***************************************** getMenuBar ******************************************/
  private Node getMenuBar()
  {
    // create menu bar
    MenuBar menuBar = new MenuBar();

    // create help menu
    Menu help = new Menu( "Help" );
    MenuItem about = new MenuItem( "About JTableFX ..." );
    about.setOnAction( event -> Utils.trace( "About JTableFX ..." ) );
    help.getItems().addAll( about );

    // create view menu
    Menu view = new Menu( "View" );
    CheckMenuItem undoWindow = new CheckMenuItem( "Undo Stack ..." );
    undoWindow.setOnAction( event -> showUndoWindow( undoWindow ) );
    view.getItems().addAll( undoWindow );

    menuBar.getMenus().addAll( help, view );
    return menuBar;
  }

  /******************************************* getTabs *******************************************/
  private Node getTabs()
  {
    // when selected tab changes, request focus for the newly selected tab contents
    TabPane tabs = new TabPane();
    tabs.getSelectionModel().selectedItemProperty().addListener(
        ( observable, oldTab, newTab ) -> Platform.runLater( () -> ( newTab.getContent() ).requestFocus() ) );

    // create demo tab pane
    m_undostack = new UndoStack();
    tabs.getTabs().add( new DemoTableDefault( m_undostack ) );
    tabs.getTabs().add( new DemoTableLarge( m_undostack ) );
    tabs.getTabs().add( new DemoTableEditable( m_undostack ) );

    return tabs;
  }

  /**************************************** getStatusBar *****************************************/
  private TextField getStatusBar()
  {
    // create status-bar for displaying status messages
    m_status = new ObservableStatus();
    TextField statusBar = new TextField();
    statusBar.setFocusTraversable( false );
    statusBar.setEditable( false );

    // display status changes on status-bar using runLater so can handle signals from other threads
    m_status.addLaterListener( ( sender, msg ) ->
    {
      statusBar.setText( m_status.getMessage() );
      statusBar.setStyle( m_status.getStyle() );
    } );
    m_status.update( Level.NORMAL, "Started" );
    m_status.clearAfterMillisecs( 2500 );

    return statusBar;
  }

  /*************************************** showUndoWindow ****************************************/
  private void showUndoWindow( CheckMenuItem menuitem )
  {
    // create undo-stack window if not already created
    if ( m_undoWindow == null )
    {
      m_undoWindow = new UndoStackWindow( m_undostack );
      m_undoWindow.setOnHiding( event -> menuitem.setSelected( false ) );
    }

    // make the undo-stack window visible or hidden
    if ( m_undoWindow.isShowing() )
      m_undoWindow.hide();
    else
    {
      m_undoWindow.show();
      m_undoWindow.toFront();
    }
  }

}
