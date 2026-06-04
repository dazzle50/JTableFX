/**************************************************************************
 *  Copyright (C) 2026 by Richard Crook                                   *
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

/**
 * Base class for JavaFX controls used as table cell editors.
 * <p>
 * Handles editor placement, focus/keyboard lifecycle, validation status, and the default undoable
 * commit to the table data model. Subclasses provide the control and convert between the control's
 * content and the cell value.
 * <p>
 * Only one editor can be active across all table views. Subclasses normally call
 * {@link #setControl(Control)} in their constructor and override {@link #getValue()} and
 * {@link #setValue(Object)}. Escape abandons editing, Enter commits if the editor is not in an
 * error state, and focus loss ends editing using {@link #endEditing()}.
 * <p>
 * Controls implementing {@link IObservableStatus} receive the table view's status object; ensure it
 * is non-null before editing. Editor instances are normally short-lived; reused instances need care
 * because {@link #open(Object, CellDrawer)} installs validation handling for {@link ExpandingField}
 * controls.
 */
abstract public class AbstractCellEditor
{
  private static AbstractCellEditor         m_editorInProgress;     // only one editor can be open across all tables
  private static ChangeListener<String>     m_removeSelectListener; // change listener to remove text selection

  private Control                           m_control;              // primary control that has focus
  private CellDrawer                        m_cell;                 // cell style and position etc
  private EventHandler<? super ScrollEvent> m_viewScrollHandler;    // table view scroll event handler

  /******************************************** open *********************************************/
  /**
   * Opens this editor over the supplied cell and initialises it with the requested value.
   * <p>
   * If another editor is already active this method returns without opening. The supplied value may
   * be the current cell value or a typed starter value used to begin replacement editing.
   *
   * @param value the value to place in the editor
   * @param cell  cell location, style, and data indexes for the edit
   * @throws IllegalStateException if no editor control has been set
   */
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

    // set current cell value first so replacing it with opening value triggers normal checks
    setValue( m_cell.getValue() );
    setValue( value );

    // if editor opening value is not same as cell value, remove default text selection
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
  /**
   * Returns whether the supplied value is valid as an initial editor value.
   *
   * @param value candidate opening value
   * @return true if the editor can start with the value
   */
  public boolean isValueValid( Object value )
  {
    // return if value is valid for starting cell editor
    if ( m_control instanceof ExpandingField field )
      return value == null || field.isAllowed( value.toString() );

    return true;
  }

  /******************************************* getValue ******************************************/
  /**
   * Returns the current value from the editor control.
   *
   * @return current editor value; the default implementation returns null
   */
  public Object getValue()
  {
    // subclasses normally override to return the control's value
    return null;
  }

  /******************************************* setValue ******************************************/
  /**
   * Sets the current value shown by the editor control.
   *
   * @param value value to show in the editor; the default implementation does nothing
   */
  public void setValue( Object value )
  {
    // subclasses normally override to update the control
  }

  /****************************************** testValue ******************************************/
  /**
   * Tests whether the table data model would accept a candidate value for this editor's cell.
   * <p>
   * The editor must already be open so the target cell is known.
   *
   * @param value candidate value to validate
   * @return null if accepted, otherwise a message describing why it was rejected
   */
  public String testValue( Object value )
  {
    // test if data model would accept new value
    int col = m_cell.dataColumn;
    int row = m_cell.dataRow;
    return m_cell.view.getData().testValue( col, row, value );
  }

  /******************************************** close ********************************************/
  /**
   * Closes this editor and optionally commits the current value.
   *
   * @param commit true to commit the current value, false to abandon it
   */
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
  /**
   * Commits the current editor value through the table view's undo stack.
   * <p>
   * Override this method when the edit should be represented by a different undo command.
   *
   * @return true if an undo command was pushed
   */
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
  /**
   * Sets the JavaFX control used by this editor and installs shared editor event handling.
   * <p>
   * Must be called before {@link #open(Object, CellDrawer)}.
   *
   * @param control primary editor control
   */
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
  /**
   * Returns the JavaFX control used by this editor.
   *
   * @return primary editor control
   */
  public Control getControl()
  {
    // return editor main control
    return m_control;
  }

  /****************************************** endEditing *****************************************/
  /**
   * Closes the active editor, committing it unless it is currently in an error state.
   */
  public static void endEditing()
  {
    // if there is a currently active editor, close it
    if ( m_editorInProgress != null )
      m_editorInProgress.close( !m_editorInProgress.isError() );
  }

  /******************************************* isError *******************************************/
  /**
   * Returns whether this editor's table status is currently an error.
   *
   * @return true if the table status is in an error state
   */
  public Boolean isError()
  {
    // return if editor in error state
    return m_cell.view.getStatus() == null ? false : m_cell.view.getStatus().isError();
  }

}
