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

package rjc.table.demo;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import rjc.table.Utils;
import rjc.table.control.ChooseField;
import rjc.table.control.DateField;
import rjc.table.control.DateTimeField;
import rjc.table.control.MonthSpinField;
import rjc.table.control.NumberSpinField;
import rjc.table.control.TimeField;
import rjc.table.demo.edit.EditableData;
import rjc.table.signal.ObservableStatus;
import rjc.table.signal.ObservableStatus.Level;
import rjc.table.undo.UndoStack;
import rjc.table.undo.UndoStackWindow;
import rjc.table.view.TableView;
import rjc.table.view.editor.EditorDateTime;
import rjc.table.view.editor.EditorTime;

/*************************************************************************************************/
/****************************** Contents of demo application window ******************************/
/*************************************************************************************************/

public class DemoContents extends GridPane
{
  private ObservableStatus m_status;     // shared status for whole demo application & shown on status bar
  private TabPane          m_tabs;       // tab-pane containing the different demo tables
  private UndoStack        m_undostack;  // shared undostack for whole demo application
  private UndoStackWindow  m_undoWindow; // window to interact with undo-stack

  /**************************************** constructor ******************************************/
  public DemoContents()
  {
    // create undostack & demo status
    m_undostack = new UndoStack();
    m_status = new ObservableStatus();

    // create demo window layout
    add( getMenuBar(), 0, 0 );

    m_tabs = getTabs();
    add( m_tabs, 0, 1 );
    setHgrow( m_tabs, Priority.ALWAYS );
    setVgrow( m_tabs, Priority.ALWAYS );

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

    MenuItem newWindow = new MenuItem( "New window ..." );
    newWindow.setOnAction( event -> openNewWindow() );

    view.getItems().addAll( undoWindow, newWindow );

    menuBar.getMenus().addAll( help, view, getBenchmarks() );
    return menuBar;
  }

  /**************************************** openNewWindow ****************************************/
  private void openNewWindow()
  {
    // open new window with different views to same data
    Stage stage = new Stage();
    stage.setScene( new Scene( getTabs() ) );
    stage.setTitle( "New window" );
    stage.show();
  }

  /******************************************* getTabs *******************************************/
  private TabPane getTabs()
  {
    // when selected tab changes, request focus for the newly selected tab contents
    TabPane tabs = new TabPane();
    tabs.getSelectionModel().selectedItemProperty().addListener(
        ( property, oldTab, newTab ) -> Platform.runLater( () -> ( newTab.getContent() ).requestFocus() ) );

    // create demo tabs with shared undostack & status
    tabs.getTabs().add( new DemoTableDefault( m_undostack, m_status ) );
    tabs.getTabs().add( new DemoTableLarge( m_undostack, m_status ) );
    tabs.getTabs().add( new DemoTableEditable( m_undostack, m_status ) );
    tabs.getTabs().add( new DemoFields( m_undostack, m_status ) );

    return tabs;
  }

  /**************************************** getStatusBar *****************************************/
  private TextField getStatusBar()
  {
    // create status-bar for displaying status messages
    TextField statusBar = new TextField();
    statusBar.setFocusTraversable( false );
    statusBar.setEditable( false );

    // display status changes on status-bar using runLater so can handle signals from other threads
    m_status.addLaterListener( ( sender, msg ) ->
    {
      statusBar.setText( m_status.getMessage() );
      statusBar.setStyle( m_status.getStyle() );
    } );
    m_status.update( Level.INFO, "Started" );
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

  /**************************************** getBenchmarks ****************************************/
  private Menu getBenchmarks()
  {
    // create benchmarks menu
    Menu benchmarks = new Menu( "Benchmarks" );

    // add the benchmarks
    addBenchmark( benchmarks, "Null", () ->
    {
    }, 1000 );

    addBenchmark( benchmarks, "Trace Here", () -> Utils.trace( "Here" ), 100 );

    addBenchmark( benchmarks, "Redraw", () ->
    {
      var tab = m_tabs.getSelectionModel().getSelectedItem();
      TableView view = (TableView) tab.getContent();
      view.redraw();
    }, 1000 );

    addBenchmark( benchmarks, "DateTimeEditor", () ->
    {
      new EditorDateTime();
    }, 1000 );

    addBenchmark( benchmarks, "EditorTime", () ->
    {
      new EditorTime();
    }, 1000 );

    addBenchmark( benchmarks, "~~~~~~~ Fields", () ->
    {
      new TimeField();
      new DateField();
      new DateTimeField();
      new MonthSpinField();
      new NumberSpinField();
      new ChooseField( EditableData.Fruit.values() );
    }, 1000 );

    addBenchmark( benchmarks, "Garbage collection", () ->
    {
      System.gc();
    }, 5 );
    return benchmarks;
  }

  /**************************************** addBenchmark *****************************************/
  private MenuItem addBenchmark( Menu menu, String name, Runnable test, int count )
  {
    // add benchmark test to menu
    MenuItem benchmark = new MenuItem( "BenchMark - " + count + " " + name );
    menu.getItems().addAll( benchmark );

    benchmark.setOnAction( event ->
    {
      // run benchmark once to get over any first-run unique delays
      test.run();

      // run benchmark requested number of times
      long[] nanos = new long[count + 1];
      Utils.trace( "######### BENCHMARK START - " + name + " " + count + " times" );
      nanos[0] = System.nanoTime();
      for ( int num = 1; num <= count; num++ )
      {
        test.run();
        nanos[num] = System.nanoTime();
      }

      // report each run duration
      long min = Long.MAX_VALUE;
      long max = Long.MIN_VALUE;
      for ( int num = 0; num < count; num++ )
      {
        long nano = nanos[num + 1] - nanos[num];
        report( "Run " + ( num + 1 ) + " duration =", nano );
        if ( nano < min )
          min = nano;
        if ( nano > max )
          max = nano;
      }

      // report total & average duration
      long total = nanos[count] - nanos[0];
      Utils.trace( "######### BENCHMARK END - " + name + " " + count + " times" );
      report( "  Total duration =", total );
      report( "Average duration =", total / count );
      report( "Minimum duration =", min );
      report( "Maximum duration =", max );
      Utils.trace( "BENCHMARK       Per second = " + String.format( "%,.1f", 1e9 * count / total ) + " " + name );
    } );

    return benchmark;
  }

  /******************************************* report ********************************************/
  private void report( String text, long nanos )
  {
    // generate trace output with nano-seconds in human readable format
    String units = " ns";
    double div = 1.0;

    if ( nanos > 1000L )
    {
      units = " \u00B5s";
      div = 1000.0;
    }

    if ( nanos > 1000000L )
    {
      units = " ms";
      div = 1000000.0;
    }

    if ( nanos > 1000000000L )
    {
      units = " s";
      div = 1000000000.0;
    }

    Utils.trace( "BENCHMARK " + text + String.format( "%8.3f", nanos / div ) + units );
  }

}
