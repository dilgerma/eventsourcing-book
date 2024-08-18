create table carts_with_products_read_model_entity (
    product_id uuid,
    aggregate_id uuid not null, primary key (aggregate_id, product_id));

create index on carts_with_products_read_model_entity (product_id)
