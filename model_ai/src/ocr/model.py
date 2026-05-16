from __future__ import annotations


def build_ocr_model(
    image_size: tuple[int, int],
    max_text_length: int,
    vocab_size: int,
    learning_rate: float = 1e-3,
):
    import tensorflow as tf

    width, height = image_size
    del max_text_length, learning_rate

    inputs = tf.keras.Input(shape=(height, width, 1), name="image")

    x = tf.keras.layers.Conv2D(64, 3, padding="same", use_bias=False)(inputs)
    x = tf.keras.layers.BatchNormalization()(x)
    x = tf.keras.layers.Activation("relu")(x)
    x = tf.keras.layers.MaxPooling2D(pool_size=(2, 2))(x)

    x = tf.keras.layers.Conv2D(128, 3, padding="same", use_bias=False)(x)
    x = tf.keras.layers.BatchNormalization()(x)
    x = tf.keras.layers.Activation("relu")(x)
    x = tf.keras.layers.MaxPooling2D(pool_size=(2, 1))(x)

    x = tf.keras.layers.SeparableConv2D(256, 3, padding="same", use_bias=False)(x)
    x = tf.keras.layers.BatchNormalization()(x)
    x = tf.keras.layers.Activation("relu")(x)
    x = tf.keras.layers.SeparableConv2D(256, 3, padding="same", use_bias=False)(x)
    x = tf.keras.layers.BatchNormalization()(x)
    x = tf.keras.layers.Activation("relu")(x)

    time_steps = width // 2
    feature_size = (height // 4) * 256
    x = tf.keras.layers.Reshape((time_steps, feature_size), name="image_to_sequence")(x)
    x = tf.keras.layers.Dense(256, activation="relu")(x)
    x = tf.keras.layers.Dropout(0.25)(x)
    x = tf.keras.layers.Bidirectional(
        tf.keras.layers.LSTM(128, return_sequences=True, dropout=0.15),
    )(x)
    x = tf.keras.layers.Bidirectional(
        tf.keras.layers.LSTM(128, return_sequences=True, dropout=0.15),
    )(x)

    # CTC reserves one extra output class for the blank token.
    outputs = tf.keras.layers.Dense(vocab_size + 1, activation="softmax", dtype="float32", name="char_probs")(x)

    return tf.keras.Model(inputs=inputs, outputs=outputs, name="crnn_ctc_ocr")
