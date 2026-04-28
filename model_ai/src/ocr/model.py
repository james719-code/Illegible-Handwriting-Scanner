from __future__ import annotations


def build_ocr_model(
    image_size: tuple[int, int],
    max_text_length: int,
    vocab_size: int,
    learning_rate: float = 1e-3,
):
    import tensorflow as tf

    width, height = image_size
    inputs = tf.keras.Input(shape=(height, width, 1))
    x = tf.keras.layers.Conv2D(32, 3, padding="same", activation="relu")(inputs)
    x = tf.keras.layers.MaxPooling2D(pool_size=2)(x)
    x = tf.keras.layers.Conv2D(64, 3, padding="same", activation="relu")(x)
    x = tf.keras.layers.MaxPooling2D(pool_size=2)(x)
    x = tf.keras.layers.Conv2D(128, 3, padding="same", activation="relu")(x)
    x = tf.keras.layers.MaxPooling2D(pool_size=2)(x)
    x = tf.keras.layers.Flatten()(x)
    x = tf.keras.layers.Dense(512, activation="relu")(x)
    x = tf.keras.layers.Dropout(0.3)(x)
    x = tf.keras.layers.Dense(max_text_length * 128, activation="relu")(x)
    x = tf.keras.layers.Reshape((max_text_length, 128))(x)
    outputs = tf.keras.layers.Dense(vocab_size, activation="softmax")(x)

    model = tf.keras.Model(inputs=inputs, outputs=outputs, name="paired_ocr_baseline")
    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=learning_rate),
        loss=tf.keras.losses.SparseCategoricalCrossentropy(),
        metrics=[tf.keras.metrics.SparseCategoricalAccuracy(name="accuracy")],
    )
    return model
