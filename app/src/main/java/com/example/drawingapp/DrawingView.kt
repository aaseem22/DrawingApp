package com.example.drawingapp

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.widget.Toast


class DrawingView(context: Context,attr:AttributeSet): View(context,attr) {
    // override the drawing view in xml file
    //mDrawPath is the path the user covers i.e, the line that users draws
    //mDrawPaint is the paint of the line
    private var mDrawPath: CustomPath?= null
    private var mCanvasBitmap: Bitmap? = null
    private var mDrawPaint: Paint? = null
    private var mCanvasPaint : Paint? = null
    private var mBrushSize : Float=0.toFloat()
    private var mColor  = Color.BLACK
    private var canvas : Canvas? = null
    // mPath is used to save the drawing after lifting the finger
    private val mPath  = ArrayList<CustomPath>()
    private val mUndoPath  = ArrayList<CustomPath>()
    private val mRedoPath  = ArrayList<CustomPath>()

    init{
        setUpDrawing()
    }

    fun onClickUndo(){
        if(mPath.size>0){
                                        // get the last position of the mUndoPath  which is the arraylist,
                                            // so last position of arraylist
            mUndoPath.add(mPath.removeAt(mPath.size-1))
            invalidate()
        }
    }
    fun onClickRedo(){
        if(mUndoPath.size>0){
            // get the last position of the mUndoPath  which is the arraylist,
            // so last position of arraylist
            mPath.add(mUndoPath.removeAt(mUndoPath.size-1))
                invalidate()


        }
    }


    private fun setUpDrawing(){
        mDrawPaint = Paint()
        mDrawPath = CustomPath(mColor,mBrushSize)
        mDrawPaint!!.color= mColor
        mDrawPaint!!.style = Paint.Style.STROKE
        mDrawPaint!!.strokeCap = Paint.Cap.ROUND
        mDrawPaint!!.strokeJoin= Paint.Join.ROUND
        mCanvasPaint= Paint(Paint.DITHER_FLAG)
       // mBrushSize= 20.toFloat()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mCanvasBitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
        canvas = Canvas(mCanvasBitmap!!)

    }

    override fun onDraw(canvas: Canvas?)
    {
        super.onDraw(canvas)
        canvas?.drawBitmap(mCanvasBitmap!!,0f,0f,mCanvasPaint)
        for(path in mPath){
            mDrawPaint!!.strokeWidth= path.brushThickness
            mDrawPaint!!.color= path.color
            canvas?.drawPath(path,mDrawPaint!!)
        }
        //if draw path is not empty
        if(!mDrawPath!!.isEmpty){
            mDrawPaint!!.strokeWidth= mDrawPath!!.brushThickness
            mDrawPaint!!.color= mDrawPath!!.color
            canvas?.drawPath(mDrawPath!!,mDrawPaint!!)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean
    {
        val touchX = event?.x
        val touchY = event?.y
        when (event?.action){
            //ACTION_DOWN is when the users touches the screen
            MotionEvent.ACTION_DOWN-> {
                mDrawPath!!.color = mColor
                mDrawPath!!.brushThickness= mBrushSize
        // to reset
                mDrawPath!!.reset()
                if (touchX != null) {
                    if (touchY != null) {
                        mDrawPath!!.moveTo(touchX,touchY)
                    }
                }
            }
            MotionEvent.ACTION_MOVE ->{
                //ACTION_MOVE is when the user moves his/her finger
                if (touchX != null) {
                    if (touchY != null) {
                        // when the finger moves , we draw a line
                        mDrawPath!!.lineTo(touchX,touchY)
                        // or mDrawPath!!.lineTo(touchX!!,touchY!!)
                    }
                }
            }
            MotionEvent.ACTION_UP ->
                //ACTION_UP is when the user lifts his/her finger
            {
             mDrawPath = CustomPath(mColor, mBrushSize)
                mPath.add(mDrawPath!!)
            }
            else -> false
        }
        //to re-draw your view from the UI thread we call invalidate
        invalidate()
        return true

    }
    fun setBrushSize(newSize: Float){
        mBrushSize= TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,newSize,resources.displayMetrics)
        mDrawPaint!!.strokeWidth=mBrushSize
    }

    fun setColor(newColor : String){
        mColor= Color.parseColor(newColor)
        mDrawPaint!!.color=mColor
    }


   internal inner class CustomPath(var color: Int, var brushThickness : Float ): Path()



}