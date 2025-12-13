CREATE TABLE product (
                         id    INT PRIMARY KEY,
                         name  VARCHAR(255),
                         price DOUBLE
);

INSERT INTO product (id, name, price) VALUES
                                          (1, 'Foo', 10.0),
                                          (2, 'Bar', 20.5),
                                          (3, 'Baz', 42.0);
