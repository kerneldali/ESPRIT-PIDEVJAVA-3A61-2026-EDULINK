<?php

namespace App\Form;
use App\Entity\Cours;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;
use Symfony\Component\Form\Extension\Core\Type\IntegerType;
use Symfony\Component\Form\Extension\Core\Type\TextareaType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Validator\Constraints\NotBlank;
use Symfony\Component\Validator\Constraints\Length;
use Symfony\Component\Validator\Constraints\GreaterThanOrEqual;

class CoursType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('title', TextType::class, [
                'label' => 'Course Title',
                'attr' => ['class' => 'form-control', 'placeholder' => 'e.g. Advanced Python'],
            ])
            ->add('description', TextareaType::class, [
                'label' => 'Short Description',
                'required' => false,
                'attr' => ['class' => 'form-control', 'rows' => 3],
                'constraints' => [
                    new Length(['max' => 5000, 'maxMessage' => 'Description cannot exceed {{ limit }} characters']),
                ],
            ])
            ->add('level', ChoiceType::class, [
                'choices' => [
                    'Beginner' => 'Beginner',
                    'Intermediate' => 'Intermediate',
                    'Advanced' => 'Advanced'
                ],
                'attr' => ['class' => 'form-control'],
                'constraints' => [
                    new NotBlank(['message' => 'Please select a level']),
                ],
            ])
            ->add('xp', IntegerType::class, [
                'label' => 'Bonus XP',
                'required' => false,
                'attr' => ['class' => 'form-control', 'min' => 0],
                'constraints' => [
                    new GreaterThanOrEqual(['value' => 0, 'message' => 'XP cannot be negative']),
                ],
            ]);

        if (!$options['hide_matiere']) {
            $builder->add('matiere_name', TextType::class, [
                'mapped' => false,
                'label' => 'Category Name',
                'required' => false,
                'attr' => ['class' => 'form-control', 'placeholder' => 'e.g. Web Development'],
                'help' => 'Enter a category name. If it exists, we will link it. If not, we will create it as Pending.',
                'constraints' => [
                    new Length(['max' => 255]),
                ],
            ]);
        }
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => Cours::class,
            'hide_matiere' => false,
        ]);
    }
}