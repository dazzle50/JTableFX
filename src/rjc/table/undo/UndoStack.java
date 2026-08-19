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

package rjc.table.undo;

import java.util.ArrayList;

import rjc.table.Utils;
import rjc.table.signal.ISignal;

/*************************************************************************************************/
/********************************** Stack of UndoCommand objects *********************************/
/*************************************************************************************************/

/**
 * Maintains an ordered history of undoable commands and the current position within that history.
 * <p>
 * Commands below {@link #getIndex()} are considered executed; commands at or above the index are
 * available for redo. Pushing a command after one or more undo operations discards the redo branch.
 * Commands supplied to {@link #push(IUndoCommand)} are expected to have already performed their
 * initial action, normally during construction or an interactive operation.
 * <p>
 * The stack can record a clean position, typically after saving, using {@link #setClean()}. If a
 * history change makes that position unreachable, the clean marker is invalidated until explicitly
 * reset. Stack changes emit an {@link ISignal} notification so controls and application state can
 * update immediately.
 * <p>
 * This class is mutable and is not thread-safe. Commands and stack operations should normally be
 * confined to the JavaFX application thread.
 *
 * @see IUndoCommand
 */
public class UndoStack implements ISignal
{
  private ArrayList<IUndoCommand> m_stack;      // list of undo commands
  private int                     m_index;      // executed commands index
  private int                     m_cleanIndex; // index when declared clean

  /**************************************** constructor ******************************************/
  /**
   * Creates an empty stack whose initial position is clean.
   */
  public UndoStack()
  {
    // initialise private variables
    m_stack = new ArrayList<>();
    clear();
  }

  /******************************************** clear ********************************************/
  /**
   * Removes all commands and marks the resulting empty stack as clean.
   */
  public void clear()
  {
    // clean the stack
    m_stack.clear();
    m_index = 0;
    setClean();
  }

  /****************************************** setClean *******************************************/
  /**
   * Marks the current history position as clean and notifies listeners.
   */
  public void setClean()
  {
    // declare current index position as being clean (for example after file saved)
    m_cleanIndex = m_index;
    signal();
  }

  /****************************************** isClean ********************************************/
  /**
   * Returns whether the current history position is the recorded clean position.
   *
   * @return {@code true} when no tracked changes exist relative to the clean position
   */
  public boolean isClean()
  {
    // stack is clean if current index equals clean index
    return m_index == m_cleanIndex;
  }

  /******************************************* getSize *******************************************/
  /**
   * Returns the total number of commands, including commands currently available for redo.
   *
   * @return number of commands in the history
   */
  public int getSize()
  {
    // return stack command list size
    return m_stack.size();
  }

  /******************************************* getIndex ******************************************/
  /**
   * Returns the number of commands currently considered executed.
   *
   * @return current history position, from zero through {@link #getSize()}
   */
  public int getIndex()
  {
    // return current executed command index
    return m_index;
  }

  /******************************************** push *********************************************/
  /**
   * Adds an already-executed command to the history.
   * <p>
   * Invalid commands are ignored. On success, any redo branch is discarded, the command becomes
   * the most recent executed command, and listeners are notified.
   *
   * @param command valid, already-executed command to record
   * @return {@code true} if the command was added; {@code false} if it was invalid
   * @throws NullPointerException if {@code command} is {@code null}
   */
  public boolean push( IUndoCommand command )
  {
    // if command not valid, then return false and don't push on stack
    if ( !command.isValid() )
      return false;

    // remove any commands from stack that haven't been actioned (i.e. above index)
    if ( m_stack.size() > m_index )
    {
      m_stack.subList( m_index, m_stack.size() ).clear();

      // if stack now shorter than clean-index it will be impossible to return to clean state
      if ( m_stack.size() < m_cleanIndex )
        m_cleanIndex = Integer.MIN_VALUE;
    }

    // add new command to stack, and update stack index
    m_stack.add( command );
    m_index = m_stack.size();
    signal();

    // return true command was successfully added to stack
    return true;
  }

  /**************************************** commandMerged ****************************************/
  /**
   * Updates stack bookkeeping after the most recent executed command has been changed in place.
   * <p>
   * A merge is a new operation, so any redo branch is no longer valid. If the clean state includes
   * the changed command, that exact state is no longer reachable and the clean marker is invalidated.
   * The caller must update and execute the command before invoking this method.
   *
   * @throws IllegalStateException if there is no executed command to merge
   */
  public void commandMerged()
  {
    // a merge requires a most recent executed command
    if ( m_index == 0 )
      throw new IllegalStateException( "No undo command to merge" );

    // a new operation after undoing invalidates the redo branch
    if ( m_stack.size() > m_index )
      m_stack.subList( m_index, m_stack.size() ).clear();

    // if the clean state includes the changed command, it is no longer reachable
    if ( m_cleanIndex >= m_index )
      m_cleanIndex = Integer.MIN_VALUE;

    signal();
  }

  /******************************************** undo *********************************************/
  /**
   * Undoes the most recent executed command, if one exists, and notifies listeners.
   * <p>
   * If the command throws an exception, the history index is left unchanged and the exception is
   * propagated to the caller.
   */
  public void undo()
  {
    // revert command and decrement index (or no action if nothing to revert)
    if ( m_index > 0 )
    {
      m_stack.get( m_index - 1 ).undo();
      m_index--;
      signal();
    }
  }

  /******************************************** redo *********************************************/
  /**
   * Redoes the next command, if one exists, and notifies listeners.
   * <p>
   * If the command throws an exception, the history index is left unchanged and the exception is
   * propagated to the caller.
   */
  public void redo()
  {
    // action command and increment index (or no action if nothing to redo)
    if ( m_index < m_stack.size() )
    {
      m_stack.get( m_index ).redo();
      m_index++;
      signal();
    }
  }

  /**************************************** getUndoCommand ***************************************/
  /**
   * Returns the most recent executed command, primarily to support command merging.
   *
   * @return command that would next be undone, or {@code null} when no command is executed
   */
  public IUndoCommand getUndoCommand()
  {
    // return current undo command (or null if none), useful for when wanting to merging commands
    if ( m_index == 0 )
      return null;
    return m_stack.get( m_index - 1 );
  }

  /***************************************** getUndoText *****************************************/
  /**
   * Returns the description of the command that would next be undone.
   *
   * @return undo description, an empty string if the command returns {@code null}, or {@code null}
   *         when no undo is available
   */
  public String getUndoText()
  {
    // return text associated with next potential undo (no need to check index as getText does)
    return getText( m_index - 1 );
  }

  /***************************************** getRedoText *****************************************/
  /**
   * Returns the description of the command that would next be redone.
   *
   * @return redo description, an empty string if the command returns {@code null}, or {@code null}
   *         when no redo is available
   */
  public String getRedoText()
  {
    // return text associated with next potential redo (no need to check index as getText does)
    return getText( m_index );
  }

  /******************************************* getText *******************************************/
  /**
   * Returns the description of the command at a history-list index.
   *
   * @param index zero-based command index
   * @return command description, an empty string if the command returns {@code null}, or
   *         {@code null} when {@code index} is outside the history
   */
  public String getText( int index )
  {
    // return text associated with command at index (or null if outside range)
    if ( index < 0 || index >= m_stack.size() )
      return null;

    // if valid index never return null
    String text = m_stack.get( index ).text();
    return text == null ? "" : text;
  }

  /****************************************** setIndex *******************************************/
  /**
   * Moves to a requested history position by undoing or redoing commands as necessary.
   * <p>
   * Values below zero move to the beginning; values above the history size move to the end.
   * Listeners are notified after movement completes. If a command throws, movement stops, the
   * index continues to represent the commands successfully processed, and the exception is
   * propagated.
   *
   * @param index requested number of executed commands
   */
  public void setIndex( int index )
  {
    // execute redo's or undo's as necessary to get to target index
    if ( index != m_index )
    {
      while ( index < m_index && m_index > 0 )
      {
        m_stack.get( m_index - 1 ).undo();
        m_index--;
      }
      while ( index > m_index && m_index < m_stack.size() )
      {
        m_stack.get( m_index ).redo();
        m_index++;
      }

      // let listeners know that stack have changed
      signal();
    }
  }

  /****************************************** toString *******************************************/
  /**
   * Returns a diagnostic representation containing the current index, clean index, size, and
   * command descriptions.
   *
   * @return diagnostic stack representation
   */
  @Override
  public String toString()
  {
    // return as string
    StringBuilder str = new StringBuilder( Utils.name( this ) );
    str.append( "[i=" + m_index );
    str.append( " c=" + m_cleanIndex );
    str.append( " s=" + m_stack.size() );
    for ( IUndoCommand command : m_stack )
      str.append( " \"" + command.text() + "\"" );
    return str.toString() + "]";
  }

}
