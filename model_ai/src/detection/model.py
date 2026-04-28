from __future__ import annotations


def dice_coefficient(y_true, y_pred, smooth: float = 1e-6):
    import tensorflow as tf

    y_true = tf.cast(y_true, tf.float32)
    y_pred = tf.cast(y_pred, tf.float32)
    intersection = tf.reduce_sum(y_true * y_pred)
    denominator = tf.reduce_sum(y_true) + tf.reduce_sum(y_pred)
    return (2.0 * intersection + smooth) / (denominator + smooth)


def build_detector_model(
    image_size: tuple[int, int],
    channels: int = 1,
    learning_rate: float = 1e-3,
):
    import tensorflow as tf

    def conv_block(x, filters: int):
        x = tf.keras.layers.Conv2D(filters, 3, padding="same", activation="relu")(x)
        x = tf.keras.layers.BatchNormalization()(x)
        x = tf.keras.layers.Conv2D(filters, 3, padding="same", activation="relu")(x)
        x = tf.keras.layers.BatchNormalization()(x)
        return x

    inputs = tf.keras.Input(shape=(image_size[1], image_size[0], channels))

    c1 = conv_block(inputs, 32)
    p1 = tf.keras.layers.MaxPooling2D()(c1)

    c2 = conv_block(p1, 64)
    p2 = tf.keras.layers.MaxPooling2D()(c2)

    bridge = conv_block(p2, 128)

    u1 = tf.keras.layers.UpSampling2D()(bridge)
    u1 = tf.keras.layers.Concatenate()([u1, c2])
    c3 = conv_block(u1, 64)

    u2 = tf.keras.layers.UpSampling2D()(c3)
    u2 = tf.keras.layers.Concatenate()([u2, c1])
    c4 = conv_block(u2, 32)

    outputs = tf.keras.layers.Conv2D(1, 1, activation="sigmoid")(c4)
    model = tf.keras.Model(inputs=inputs, outputs=outputs, name="text_region_detector")
    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=learning_rate),
        loss="binary_crossentropy",
        metrics=[dice_coefficient, tf.keras.metrics.BinaryAccuracy(name="accuracy")],
    )
    return model
