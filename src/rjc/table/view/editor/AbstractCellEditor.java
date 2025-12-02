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

package rjc.table.view.editor;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.control.Control;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import rjc.table.control.ExpandingField;
import rjc.table.control.IObservableStatus;
import rjc.table.data.TableData;
import rjc.table.signal.ObservableStatus.Level;
import rjc.table.undo.commands.CommandSetValue;
import rjc.table.view.TableView;
import rjc.table.view.cell.CellDrawer;

/*************************************************************************************************/
/************************** Abstract base class for a table cell editor **************************/
/*************************************************************************************************/

abstract public class AbstractCellEditor
{
  private static AbstractCellEditor         m_editorInProgress;     // only one editor can be open at any time
  private static ChangeListener<String>     m_removeSelectListener; // change listener to remove text selection

  private Control                           m_control;              // primary control that has focus
  private CellDrawer                        m_cell;                 // cell style and position etc
  private EventHandler<? super ScrollEvent> m_viewScrollHandler;    // table view scroll event handler

  /******************************************** open *********************************************/
  public void open( Object value, CellDrawer cell )
  {
    // do nothing if an editor already open
    if ( m_editorInProgress != null )
      return;

    // open editor - check editor is set
    if ( m_control == null )
      throw new IllegalStateException( "Editor control not set" );

    // set editor position & size
    m_cell = cell;
    m_control.setLayoutX( cell.x - 1 );
    m_control.setLayoutY( cell.y - 1 );
    m_control.setMaxSize( cell.w + 1, cell.h + 1 );
    m_control.setMinSize( cell.w + 1, cell.h + 1 );

    // if control derived from ExpandingField can set min & max width, insets and font
    TableView view = cell.view;
    if ( m_control instanceof ExpandingField field )
    {
      double max = view.getCanvas().getWidth() - cell.x;
      double min = cell.w + 1;
      if ( min > max )
        min = max;
      field.setPadding( cell.getZoomTextInsets() );
      field.setFont( cell.getZoomFont() );
      field.setWidths( min, max );

      // also when text changes, test value (but only if error not already detected)
      field.textProperty().addListener( ( property, oldText, newText ) ->
      {
        if ( m_editorInProgress != null && field.getStatus().getSeverity() == Level.INFO )
        {
          var decline = m_editorInProgress.testValue( m_editorInProgress.getValue() );
          if ( decline == null )
          {
            field.getStatus().setSeverity( Level.INFO );
            field.setStyle( field.getStatus().getStyle() );
          }
          else
          {
            field.getStatus().update( Level.ERROR, decline );
            field.setStyle( field.getStatus().getStyle() );
          }
        }
      } );
    }

    // if control has observable status, set to view observable status
    if ( m_control instanceof IObservableStatus field )
      field.setStatus( cell.view.getStatus() );

    // get copy then remove the table view scroll event handler
    m_viewScrollHandler = cell.view.getOnScroll();
    cell.view.setOnScroll( null );

    // display editor, give focus, and set editor value
    m_editorInProgress = this;
    view.add( m_control );
    m_control.requestFocus();

    // set editor value to cell value before opening value to ensure any value changes are checked
    setValue( m_cell.getValue() );
    setValue( value );

    // if editor opening value is not same as cell value, removed default text selection
    if ( m_control instanceof TextInputControl textInput )
      if ( value != m_cell.getValue() )
      {
        // ensure change listener has been created
        if ( m_removeSelectListener == null )
          m_removeSelectListener = new ChangeListener<String>()
          {
            @Override
            public void changed( ObservableValue<? extends String> observable, String oldText, String newText )
            {
              observable.removeListener( m_removeSelectListener );
              ( (TextInputControl) m_editorInProgress.getControl() ).end();
            }
          };

        // remove default text selection (via listener so happens after selection)
        textInput.selectedTextProperty().addListener( m_removeSelectListener );
      }
  }

  /***************************************** isValueValid ****************************************/
  public boolean isValueValid( Object value )
  {
    // return if value is valid for starting cell editor
    if ( m_control instanceof ExpandingField field )
      return value == null || field.isAllowed( value.toString() );

    return true;
  }

  /******************************************* getValue ******************************************/
  public Object getValue()
  {
    // get editor value - normally overloaded
    return null;
  }

  /******************************************* setValue ******************************************/
  public void setValue( Object value )
  {
    // set editor value - normally overloaded
  }

  /****************************************** testValue ******************************************/
  public String testValue( Object value )
  {
    // test if data model would accept new value
    int col = m_cell.dataColumn;
    int row = m_cell.dataRow;
    return m_cell.view.getData().testValue( col, row, getValue() );
  }

  /******************************************** close ********************************************/
  public void close( boolean commit )
  {
    // clear any error message, remove control from table, and give focus back to table
    m_editorInProgress = null;
    if ( m_cell.view.getStatus() != null )
      m_cell.view.getStatus().clear();
    m_cell.view.requestFocus();
    m_cell.view.remove( m_control );
    m_cell.view.setOnScroll( m_viewScrollHandler );

    if ( commit )
      commit();
  }

  /******************************************** commit ********************************************/
  protected boolean commit()
  {
    // attempt to commit editor value to data source - override if different undo-command wanted
    TableData data = m_cell.view.getData();
    int dataColumn = m_cell.dataColumn;
    int dataRow = m_cell.dataRow;

    // push new command on undo-stack to update cell value
    var command = new CommandSetValue( data, dataColumn, dataRow, getValue() );
    return m_cell.view.getUndoStack().push( command );
  }

  /***************************************** setControl ******************************************/
  public void setControl( Control control )
  {
    // set editor main control
    m_control = control;

    // add listener to end editing if focus lost
    m_control.focusedProperty().addListener( ( property, oldFocus, newFocus ) ->
    {
      if ( !newFocus )
        AbstractCellEditor.endEditing();
    } );

    // add key press event handler to close if escape or enter pressed
    m_control.addEventHandler( KeyEvent.KEY_PRESSED, event ->
    {
      if ( event.getCode() == KeyCode.ESCAPE )
        m_editorInProgress.close( false ); // abandon edit
      if ( event.getCode() == KeyCode.ENTER && !m_editorInProgress.isError() )
        m_editorInProgress.close( true ); // commit edit
      if ( event.getCode() == KeyCode.ENTER )
        event.consume(); // consume event so not propagated to table-view
    } );
  }

  /***************************************** getControl ******************************************/
  public Control getControl()
  {
    // return editor main control
    return m_control;
  }

  /****************************************** endEditing *****************************************/
  public static void endEditing()
  {
    // if there is a currently active editor, close it
    if ( m_editorInProgress != null )
      m_editorInProgress.close( !m_editorInProgress.isError() );
  }

  /******************************************* isError *******************************************/
  public Boolean isError()
  {
    // return if editor in error state
    return m_cell.view.getStatus() == null ? false : m_cell.view.getStatus().isError();
  }

}
