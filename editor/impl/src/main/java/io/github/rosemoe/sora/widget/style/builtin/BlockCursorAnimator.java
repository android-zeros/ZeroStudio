package io.github.rosemoe.sora.widget.style.builtin;

import android.animation.ValueAnimator;
import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.widget.EditorRenderer;
import io.github.rosemoe.sora.widget.style.CursorAnimator;

/**
 * A block style cursor animator.
 * This is a customized version of MoveCursorAnimator.
 */
public class BlockCursorAnimator extends MoveCursorAnimator {

    public BlockCursorAnimator(CodeEditor editor) {
        super(editor);
        // Change cursor width to be character-like
        // A better approach would be to measure the character under the cursor
        editor.setCursorWidth(editor.getTextPaint().measureText("M"));
    }
}