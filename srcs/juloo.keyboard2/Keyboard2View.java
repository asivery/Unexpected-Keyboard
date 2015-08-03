package juloo.keyboard2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import java.util.LinkedList;

public class Keyboard2View extends View
	implements View.OnTouchListener
{
	private static final float	KEY_PER_ROW = 10;

	private static final float	SUB_VALUE_DIST = 6f;

	private static final long	VIBRATE_LONG = 25;
	private static final long	VIBRATE_MIN_INTERVAL = 100;

	private static final long	LONGPRESS_TIMEOUT = 800;
	private static final long	LONGPRESS_INTERVAL = 90;

	private Keyboard2		_ime;
	private KeyboardData	_keyboard;

	private LinkedList<KeyDown>	_downKeys = new LinkedList<KeyDown>();

	private int				_flags = 0;

	private Vibrator		_vibratorService;
	private long			_lastVibration = 0;

	private float			_verticalMargin;
	private float			_horizontalMargin;
	private float			_keyWidth;
	private float			_keyHeight;
	private float			_keyPadding;
	private float			_keyBgPadding;
	private float			_keyRound;

	private Paint			_keyBgPaint = new Paint();
	private Paint			_keyDownBgPaint = new Paint();
	private Paint			_keyLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private Paint			_keyLabelLockedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private Paint			_keySubLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

	public Keyboard2View(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		_vibratorService = (Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);
		_verticalMargin = getResources().getDimension(R.dimen.vertical_margin);
		_horizontalMargin = getResources().getDimension(R.dimen.horizontal_margin);
		_keyHeight = getResources().getDimension(R.dimen.key_height);
		_keyPadding = getResources().getDimension(R.dimen.key_padding);
		_keyBgPadding = getResources().getDimension(R.dimen.key_bg_padding);
		_keyRound = getResources().getDimension(R.dimen.key_round);
		_keyBgPaint.setColor(getResources().getColor(R.color.key_bg));
		_keyDownBgPaint.setColor(getResources().getColor(R.color.key_down_bg));
		_keyLabelPaint.setColor(getResources().getColor(R.color.key_label));
		_keyLabelPaint.setTextSize(getResources().getDimension(R.dimen.label_text_size));
		_keyLabelPaint.setTextAlign(Paint.Align.CENTER);
		_keyLabelLockedPaint.setColor(getResources().getColor(R.color.key_label_locked));
		_keyLabelLockedPaint.setTextSize(getResources().getDimension(R.dimen.label_text_size));
		_keyLabelLockedPaint.setTextAlign(Paint.Align.CENTER);
		_keySubLabelPaint.setColor(getResources().getColor(R.color.key_sub_label));
		_keySubLabelPaint.setTextSize(getResources().getDimension(R.dimen.sublabel_text_size));
		setOnTouchListener(this);
	}

	public void			setKeyboard(Keyboard2 ime, KeyboardData keyboardData)
	{
		_ime = ime;
		_keyboard = keyboardData;
	}

	@Override
	public boolean		onTouch(View v, MotionEvent event)
	{
		float				x;
		float				y;
		float				keyW;
		int					p;

		switch (event.getActionMasked())
		{
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_POINTER_UP:
			onTouchUp(event.getPointerId(event.getActionIndex()));
			break ;
		case MotionEvent.ACTION_DOWN:
		case MotionEvent.ACTION_POINTER_DOWN:
			p = event.getActionIndex();
			onTouchDown(event.getX(p), event.getY(p), event.getPointerId(p));
			break ;
		case MotionEvent.ACTION_MOVE:
			for (p = 0; p < event.getPointerCount(); p++)
				onTouchMove(event.getX(p), event.getY(p), event.getPointerId(p));
			break ;
		default:
			return (false);
		}
		return (true);
	}

	private KeyDown		getKeyDown(int pointerId)
	{
		for (KeyDown k : _downKeys)
		{
			if (k.pointerId == pointerId)
				return (k);
		}
		return (null);
	}

	private KeyDown		getKeyDown(KeyboardData.Key key)
	{
		for (KeyDown k : _downKeys)
		{
			if (k.key == key)
				return (k);
		}
		return (null);
	}

	private void		onTouchMove(float moveX, float moveY, int pointerId)
	{
		KeyDown				key = getKeyDown(pointerId);
		KeyValue			newValue;

		if (key != null)
		{
			moveX -= key.downX;
			moveY -= key.downY;
			if ((Math.abs(moveX) + Math.abs(moveY)) < SUB_VALUE_DIST)
				newValue = key.key.key0;
			else if (moveX < 0)
				newValue = (moveY < 0) ? key.key.key1 : key.key.key3;
			else if (moveY < 0)
				newValue = key.key.key2;
			else
				newValue = key.key.key4;
			if (newValue != null && newValue != key.value)
			{
				key.setValue(newValue);
				updateFlags();
				vibrate();
			}
			else if (key.value != null && (key.flags & KeyValue.FLAG_NOCHAR) == 0)
			{
				long		now = System.currentTimeMillis();

				if (now >= key.nextPress)
				{
					key.nextPress = now + LONGPRESS_INTERVAL;
					_ime.handleKeyUp(key.value, _flags);
					vibrate();
				}
			}
		}
	}

	private void		onTouchDown(float touchX, float touchY, int pointerId)
	{
		float				x;
		float				y;
		float				keyW;

		y = _verticalMargin - _keyHeight;
		for (KeyboardData.Row row : _keyboard.getRows())
		{
			y += _keyHeight;
			if (touchY < y || touchY >= (y + _keyHeight))
				continue ;
			x = (KEY_PER_ROW * _keyWidth - row.getWidth(_keyWidth)) / 2 + _horizontalMargin;
			for (KeyboardData.Key key : row)
			{
				keyW = _keyWidth * key.width;
				if (touchX >= x && touchX < (x + keyW))
				{
					KeyDown down = getKeyDown(key);
					if (down != null)
					{
						if ((down.flags & KeyValue.FLAG_LOCK) != 0)
						{
							down.flags ^= KeyValue.FLAG_LOCK;
							down.flags |= KeyValue.FLAG_LOCKED;
						}
						else if (down.pointerId == -1)
							down.pointerId = pointerId;
					}
					else
						_downKeys.add(new KeyDown(pointerId, key, touchX, touchY));
					vibrate();
					updateFlags();
					invalidate();
					return ;
				}
				x += keyW;
			}
		}
	}

	private void		onTouchUp(int pointerId)
	{
		KeyDown				k = getKeyDown(pointerId);

		if (k != null)
		{
			if ((k.flags & KeyValue.FLAG_KEEP_ON) != 0)
			{
				k.flags ^= KeyValue.FLAG_KEEP_ON;
				k.pointerId = -1;
				return ;
			}
			for (int i = 0; i < _downKeys.size(); i++)
			{
				KeyDown downKey = _downKeys.get(i);
				if (downKey.pointerId == -1 && (downKey.flags & KeyValue.FLAG_LOCKED) == 0)
					_downKeys.remove(i--);
				else if ((downKey.flags & KeyValue.FLAG_KEEP_ON) != 0)
					downKey.flags ^= KeyValue.FLAG_KEEP_ON;
			}
			if (k.value != null && (k.flags & (KeyValue.FLAG_LOCKED | KeyValue.FLAG_NOCHAR)) == 0)
				_ime.handleKeyUp(k.value, _flags);
			_downKeys.remove(k);
			updateFlags();
			invalidate();
			return ;
		}
	}

	private void		updateFlags()
	{
		_flags = 0;
		for (KeyDown k : _downKeys)
			_flags |= k.flags;
	}

	private void		vibrate()
	{
		long		now = System.currentTimeMillis();

		if ((now - _lastVibration) > VIBRATE_MIN_INTERVAL)
		{
			_lastVibration = now;
			try
			{
				_vibratorService.vibrate(VIBRATE_LONG);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	@Override
	public void			onMeasure(int wSpec, int hSpec)
	{
		DisplayMetrics		dm = getContext().getResources().getDisplayMetrics();
		int					height;

		if (_keyboard.getRows() == null)
			height = 0;
		else
			height = (int)(_keyHeight * ((float)_keyboard.getRows().size())
				+ (_verticalMargin * 2));
		setMeasuredDimension(dm.widthPixels, height);
		_keyWidth = (getWidth() - (_horizontalMargin * 2)) / KEY_PER_ROW;
	}

	@Override
	protected void		onDraw(Canvas canvas)
	{
		float				x;
		float				y;
		boolean				upperCase = ((_flags & KeyValue.FLAG_SHIFT) != 0);

		y = _verticalMargin;
		for (KeyboardData.Row row : _keyboard.getRows())
		{
			x = (KEY_PER_ROW * _keyWidth - row.getWidth(_keyWidth)) / 2f + _horizontalMargin;
			for (KeyboardData.Key k : row)
			{
				float keyW = _keyWidth * k.width;
				KeyDown keyDown = getKeyDown(k);
				if (keyDown != null)
					canvas.drawRect(x + _keyBgPadding, y + _keyBgPadding,
						x + keyW - _keyBgPadding, y + _keyHeight - _keyBgPadding, _keyDownBgPaint);
				else
					canvas.drawRoundRect(new RectF(x + _keyBgPadding, y + _keyBgPadding,
						x + keyW - _keyBgPadding, y + _keyHeight - _keyBgPadding), _keyRound, _keyRound, _keyBgPaint);
				if (k.key0 != null)
					canvas.drawText(k.key0.getSymbol(upperCase), keyW / 2f + x,
						(_keyHeight + _keyLabelPaint.getTextSize()) / 2f + y,
						(keyDown != null && (keyDown.flags & KeyValue.FLAG_LOCKED) != 0)
							? _keyLabelLockedPaint : _keyLabelPaint);
				float subPadding = _keyBgPadding + _keyPadding;
				_keySubLabelPaint.setTextAlign(Paint.Align.LEFT);
				if (k.key1 != null)
					canvas.drawText(k.key1.getSymbol(upperCase), x + subPadding,
						y + subPadding - _keySubLabelPaint.ascent(), _keySubLabelPaint);
				if (k.key3 != null)
					canvas.drawText(k.key3.getSymbol(upperCase), x + subPadding,
						y + _keyHeight - subPadding - _keySubLabelPaint.descent(), _keySubLabelPaint);
				_keySubLabelPaint.setTextAlign(Paint.Align.RIGHT);
				if (k.key2 != null)
					canvas.drawText(k.key2.getSymbol(upperCase), x + keyW - subPadding,
						y + subPadding - _keySubLabelPaint.ascent(), _keySubLabelPaint);
				if (k.key4 != null)
					canvas.drawText(k.key4.getSymbol(upperCase), x + keyW - subPadding,
						y + _keyHeight - subPadding - _keySubLabelPaint.descent(), _keySubLabelPaint);
				x += keyW;
			}
			y += _keyHeight;
		}
	}

	private class KeyDown
	{
		public int				pointerId;
		public KeyValue			value;
		public KeyboardData.Key	key;
		public float			downX;
		public float			downY;
		public int				flags;
		public long				nextPress;

		public KeyDown(int pointerId, KeyboardData.Key key, float x, float y)
		{
			this.pointerId = pointerId;
			this.key = key;
			downX = x;
			downY = y;
			setValue(key.key0);
		}

		public void				setValue(KeyValue v)
		{
			value = v;
			flags = (value == null) ? 0 : v.getFlags();
			nextPress = System.currentTimeMillis() + LONGPRESS_TIMEOUT;
		}
	}
}